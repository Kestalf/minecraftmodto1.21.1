package io.github.kestalf.futurebackport.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Pulls the textures, models, block states, particles, sounds and translations that are new in
 * {@code sourceVersion} (compared to {@code baseVersion}) out of the official Minecraft client and
 * re-namespaces them for the mod. Nothing downloaded here is committed to the repository.
 */
public abstract class ImportVanillaAssetsTask extends DefaultTask {
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final double[] VALID_ANGLES = {-45, -22.5, 0, 22.5, 45};
    // Registry-backed translation keys that have to follow the mod's namespace.
    private static final Pattern NAMESPACED_KEY = Pattern.compile(
            "^(block|item|entity|effect|enchantment|biome|jukebox_song|painting|trim_material|trim_pattern|instrument|filled_map)\\.minecraft\\.(.+)$");
    // Item model index of every vanilla trim material in 1.21.1 (used by the "trim_type" item predicate).
    private static final Map<String, Double> TRIM_INDEX = Map.of(
            "minecraft:quartz", 0.1, "minecraft:iron", 0.2, "minecraft:netherite", 0.3, "minecraft:redstone", 0.4,
            "minecraft:copper", 0.5, "minecraft:gold", 0.6, "minecraft:emerald", 0.7, "minecraft:diamond", 0.8,
            "minecraft:lapis", 0.9, "minecraft:amethyst", 1.0);

    @Input
    public abstract Property<String> getModId();

    @Input
    public abstract Property<String> getSourceVersion();

    @Input
    public abstract Property<String> getBaseVersion();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRemapFile();

    /** Resource roots whose files take precedence over imported ones (imported duplicates are skipped). */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getOverrideRoots();

    /** Directory with hand-written {@code <lang>.json} files merged on top of the imported translations. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getExtraLangDir();

    @Internal
    public abstract DirectoryProperty getCacheDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    private String ns;
    private Path out;
    private Path cache;
    private Set<String> overrides;
    private int written;

    @TaskAction
    public void run() throws IOException {
        ns = getModId().get();
        out = getOutputDir().get().getAsFile().toPath();
        cache = getCacheDir().get().getAsFile().toPath();
        deleteRecursively(out);
        Files.createDirectories(out);
        overrides = collectOverrides();
        Config config = Config.load(getRemapFile().isPresent() ? getRemapFile().get().getAsFile() : null);

        JsonObject manifest = readJson(download(MANIFEST_URL, cache.resolve("version_manifest_v2.json"), null, true));
        Version src = resolveVersion(manifest, getSourceVersion().get());
        Version base = resolveVersion(manifest, getBaseVersion().get());

        try (ZipFile srcJar = new ZipFile(src.jar.toFile()); ZipFile baseJar = new ZipFile(base.jar.toFile())) {
            Assets srcAssets = new Assets(srcJar, src.index);
            Assets baseAssets = new Assets(baseJar, base.index);
            Importer importer = new Importer(srcAssets, baseAssets, config);
            importer.textures();
            importer.models();
            importer.blockStates();
            importer.itemModels();
            importer.particles();
            importer.extraCopies();
            importer.layoutConversions();
            importer.sounds();
            importer.languages();
        }
        getLogger().lifecycle("Imported {} vanilla {} asset files into {}", written, getSourceVersion().get(), out);
    }

    // ---------------------------------------------------------------- importer

    private final class Importer {
        private final Assets src;
        private final Assets base;
        private final Config config;
        private final Set<String> newTextures = new HashSet<>();
        private final Set<String> newModels = new HashSet<>();

        Importer(Assets src, Assets base, Config config) {
            this.src = src;
            this.base = base;
            this.config = config;
        }

        void textures() throws IOException {
            for (String name : src.jarEntries("assets/minecraft/textures/")) {
                String rel = name.substring("assets/minecraft/textures/".length());
                if (!(rel.endsWith(".png") || rel.endsWith(".png.mcmeta"))) continue;
                boolean forced = config.forceTextures.contains(rel.replace(".mcmeta", "").replace(".png", ""));
                if (!forced && base.hasJarEntry(name)) continue;
                if (config.isTextureExcluded(rel)) continue;
                if (rel.endsWith(".png")) newTextures.add(rel.substring(0, rel.length() - 4));
                write("assets/" + ns + "/textures/" + rel, src.readJar(name));
            }
        }

        void models() throws IOException {
            List<String> names = new ArrayList<>();
            for (String name : src.jarEntries("assets/minecraft/models/")) {
                if (name.endsWith(".json") && !base.hasJarEntry(name)) {
                    names.add(name);
                    newModels.add(name.substring("assets/minecraft/models/".length(), name.length() - 5));
                }
            }
            for (String name : names) {
                JsonObject model = parse(src.readJar(name)).getAsJsonObject();
                rewriteModel(model);
                JsonObject patch = config.modelPatches.get(name.substring("assets/minecraft/models/".length(), name.length() - 5));
                if (patch != null) patch.asMap().forEach(model::add);
                writeJson("assets/" + ns + "/models/" + name.substring("assets/minecraft/models/".length()), model);
            }
        }

        void blockStates() throws IOException {
            for (String name : src.jarEntries("assets/minecraft/blockstates/")) {
                String id = fileId(name);
                if (base.hasJarEntry(name) || config.excludeBlocks.contains(id)) continue;
                JsonElement state = parse(src.readJar(name));
                rewriteModelRefs(state);
                writeJson("assets/" + ns + "/blockstates/" + id + ".json", state);
            }
        }

        void itemModels() throws IOException {
            for (String name : src.jarEntries("assets/minecraft/items/")) {
                String id = fileId(name);
                if (base.hasJarEntry("assets/minecraft/models/item/" + id + ".json")) continue;
                if (config.excludeItems.contains(id) || config.excludeBlocks.contains(id)) continue;
                String target = "assets/" + ns + "/models/item/" + id + ".json";
                JsonObject definition = parse(src.readJar(name)).getAsJsonObject().getAsJsonObject("model");
                JsonObject converted = convertItemModel(definition);
                if (converted == null) continue;
                String selfRef = ns + ":item/" + id;
                if (converted.size() == 1 && converted.has("parent") && converted.get("parent").getAsString().equals(selfRef)) {
                    continue; // the imported models/item/<id>.json already is the item model
                }
                Path existingPath = out.resolve(target);
                if (Files.exists(existingPath)) {
                    if (converted.has("overrides") && converted.get("parent").getAsString().equals(selfRef)) {
                        // Trim overrides: merge them into the imported model.
                        JsonObject existing = parse(Files.readAllBytes(existingPath)).getAsJsonObject();
                        existing.add("overrides", converted.get("overrides"));
                        Files.writeString(existingPath, GSON.toJson(existing), StandardCharsets.UTF_8);
                        continue;
                    }
                    // The definition switches between several models, one of which has the item's own name:
                    // keep that one under another name and let the item model point to it.
                    String renamed = id + "_default";
                    Files.move(existingPath, existingPath.resolveSibling(renamed + ".json"));
                    converted = JsonParser.parseString(GSON.toJson(converted).replace("\"" + selfRef + "\"", "\"" + ns + ":item/" + renamed + "\"")).getAsJsonObject();
                }
                writeJson(target, converted);
            }
        }

        /** Converts a 1.21.4+ item model definition into a 1.21.1 (NeoForge) item model. */
        private JsonObject convertItemModel(JsonObject node) {
            String type = node.get("type").getAsString();
            switch (type) {
                case "minecraft:model": {
                    JsonObject result = new JsonObject();
                    result.addProperty("parent", modelRef(node.get("model").getAsString()));
                    return result;
                }
                case "minecraft:select": {
                    String property = node.has("property") ? node.get("property").getAsString() : "";
                    JsonObject fallback = node.has("fallback") ? convertItemModel(node.getAsJsonObject("fallback")) : null;
                    JsonArray cases = node.getAsJsonArray("cases");
                    if (property.equals("minecraft:display_context") && fallback != null) {
                        JsonObject result = new JsonObject();
                        result.addProperty("loader", "neoforge:separate_transforms");
                        result.add("base", fallback);
                        JsonObject perspectives = new JsonObject();
                        for (JsonElement c : cases) {
                            JsonObject caseModel = convertItemModel(c.getAsJsonObject().getAsJsonObject("model"));
                            if (caseModel == null) continue;
                            for (String when : strings(c.getAsJsonObject().get("when"))) {
                                if (!when.equals("on_shelf")) perspectives.add(when, caseModel);
                            }
                        }
                        result.add("perspectives", perspectives);
                        return result;
                    }
                    if (property.equals("minecraft:trim_material") && fallback != null) {
                        List<JsonObject> overrides = new ArrayList<>();
                        for (JsonElement c : cases) {
                            JsonObject caseModel = convertItemModel(c.getAsJsonObject().getAsJsonObject("model"));
                            for (String when : strings(c.getAsJsonObject().get("when"))) {
                                Double index = TRIM_INDEX.get(when);
                                if (index == null || caseModel == null) continue;
                                JsonObject override = new JsonObject();
                                JsonObject predicate = new JsonObject();
                                predicate.addProperty("trim_type", index);
                                override.add("predicate", predicate);
                                override.add("model", caseModel.get("parent"));
                                overrides.add(override);
                            }
                        }
                        overrides.sort(Comparator.comparingDouble(o -> o.getAsJsonObject("predicate").get("trim_type").getAsDouble()));
                        JsonArray array = new JsonArray();
                        overrides.forEach(array::add);
                        fallback.add("overrides", array);
                        return fallback;
                    }
                    if (fallback != null) return fallback;
                    return cases.isEmpty() ? null : convertItemModel(cases.get(0).getAsJsonObject().getAsJsonObject("model"));
                }
                case "minecraft:condition":
                    return convertItemModel(node.getAsJsonObject("on_false"));
                case "minecraft:range_dispatch":
                    return node.has("fallback") ? convertItemModel(node.getAsJsonObject("fallback")) : null;
                case "minecraft:composite": {
                    for (JsonElement child : node.getAsJsonArray("models")) {
                        JsonObject converted = convertItemModel(child.getAsJsonObject());
                        if (converted != null) return converted;
                    }
                    return null;
                }
                case "minecraft:special": {
                    // Rendered by code (see "patch_models" in vanilla-assets.json for the builtin/entity parents).
                    if (!node.has("base")) return null;
                    JsonObject result = new JsonObject();
                    result.addProperty("parent", modelRef(node.get("base").getAsString()));
                    return result;
                }
                default:
                    return null;
            }
        }

        void particles() throws IOException {
            for (String name : src.jarEntries("assets/minecraft/particles/")) {
                if (base.hasJarEntry(name)) continue;
                JsonObject particle = parse(src.readJar(name)).getAsJsonObject();
                if (particle.has("textures")) {
                    JsonArray textures = new JsonArray();
                    for (JsonElement t : particle.getAsJsonArray("textures")) {
                        String path = stripNamespace(t.getAsString());
                        textures.add(newTextures.contains("particle/" + path) ? ns + ":" + path : "minecraft:" + path);
                    }
                    particle.add("textures", textures);
                }
                writeJson("assets/" + ns + "/particles/" + fileId(name) + ".json", particle);
            }
        }

        void extraCopies() throws IOException {
            for (Map.Entry<String, String> copy : config.copies.entrySet()) {
                String from = "assets/minecraft/" + copy.getKey();
                if (!src.hasJarEntry(from)) {
                    getLogger().warn("Vanilla asset {} does not exist in {}", from, getSourceVersion().get());
                    continue;
                }
                write("assets/" + ns + "/" + copy.getValue(), src.readJar(from));
            }
        }

        /**
         * Some textures changed their layout (e.g. sign textures moved from entity/signs to block/*_sign). The pixel
         * mapping between the two layouts is learned from vanilla variants that exist in both versions and then applied
         * to the new variants.
         */
        void layoutConversions() throws IOException {
            for (LayoutConversion conversion : config.layoutConversions) {
                List<BufferedImage> olds = new ArrayList<>();
                List<BufferedImage> news = new ArrayList<>();
                for (String example : conversion.examples) {
                    olds.add(readImage(base.readJar("assets/minecraft/textures/" + conversion.oldPattern.replace("{}", example))));
                    news.add(readImage(src.readJar("assets/minecraft/textures/" + conversion.newPattern.replace("{}", example))));
                }
                int width = olds.get(0).getWidth();
                int height = olds.get(0).getHeight();
                Map<String, int[]> newIndex = new java.util.HashMap<>();
                BufferedImage firstNew = news.get(0);
                for (int y = firstNew.getHeight() - 1; y >= 0; y--) {
                    for (int x = firstNew.getWidth() - 1; x >= 0; x--) {
                        newIndex.put(pixelKey(news, x, y), new int[]{x, y});
                    }
                }
                int[][][] mapping = new int[width][height][];
                List<int[]> unresolved = new ArrayList<>();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        boolean transparent = true;
                        for (BufferedImage image : olds) transparent &= (image.getRGB(x, y) >>> 24) == 0;
                        if (transparent) continue;
                        int[] source = newIndex.get(pixelKey(olds, x, y));
                        if (source != null) mapping[x][y] = source;
                        else unresolved.add(new int[]{x, y});
                    }
                }
                for (int[] p : unresolved) {
                    int[] best = null;
                    int bestDistance = Integer.MAX_VALUE;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int d = Math.abs(x - p[0]) + Math.abs(y - p[1]);
                            if (mapping[x][y] != null && d < bestDistance) {
                                bestDistance = d;
                                best = mapping[x][y];
                            }
                        }
                    }
                    mapping[p[0]][p[1]] = best;
                }
                for (String target : conversion.targets) {
                    String newPath = "assets/minecraft/textures/" + conversion.newPattern.replace("{}", target);
                    if (!src.hasJarEntry(newPath)) {
                        getLogger().warn("Texture {} does not exist in {}", newPath, getSourceVersion().get());
                        continue;
                    }
                    BufferedImage source = readImage(src.readJar(newPath));
                    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int[] from = mapping[x][y];
                            if (from != null) result.setRGB(x, y, source.getRGB(from[0], from[1]));
                        }
                    }
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(result, "png", bytes);
                    write("assets/" + ns + "/textures/" + conversion.oldPattern.replace("{}", target), bytes.toByteArray());
                }
            }
        }

        private static String pixelKey(List<BufferedImage> images, int x, int y) {
            StringBuilder key = new StringBuilder();
            for (BufferedImage image : images) {
                int argb = image.getRGB(x, y);
                key.append((argb >>> 24) == 0 ? 0 : argb).append(',');
            }
            return key.toString();
        }

        private static BufferedImage readImage(byte[] data) throws IOException {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
        }

        void sounds() throws IOException {
            JsonObject srcSounds = parse(src.readObject("minecraft/sounds.json")).getAsJsonObject();
            JsonObject baseSounds = parse(base.readObject("minecraft/sounds.json")).getAsJsonObject();
            Set<String> newEvents = new HashSet<>();
            for (String event : srcSounds.keySet()) {
                if (!baseSounds.has(event) && !config.isSoundExcluded(event)) newEvents.add(event);
            }
            JsonObject result = new JsonObject();
            for (String event : new TreeMap<>(srcSounds.asMap()).keySet()) {
                if (!newEvents.contains(event)) continue;
                JsonObject definition = srcSounds.getAsJsonObject(event).deepCopy();
                JsonArray sounds = new JsonArray();
                for (JsonElement entry : definition.getAsJsonArray("sounds")) {
                    JsonObject sound = entry.isJsonObject() ? entry.getAsJsonObject() : new JsonObject();
                    String soundName = entry.isJsonObject() ? sound.get("name").getAsString() : entry.getAsString();
                    String path = stripNamespace(soundName);
                    String resolved;
                    if (sound.has("type") && sound.get("type").getAsString().equals("event")) {
                        resolved = newEvents.contains(path) ? ns + ":" + path : "minecraft:" + path;
                    } else {
                        String object = "minecraft/sounds/" + path + ".ogg";
                        String srcHash = src.objectHash(object);
                        if (srcHash == null) {
                            getLogger().warn("Missing sound file {}", object);
                            continue;
                        }
                        if (srcHash.equals(base.objectHash(object))) {
                            resolved = "minecraft:" + path;
                        } else {
                            write("assets/" + ns + "/sounds/" + path + ".ogg", src.readObject(object));
                            resolved = ns + ":" + path;
                        }
                    }
                    if (entry.isJsonObject()) {
                        sound.addProperty("name", resolved);
                        sounds.add(sound);
                    } else {
                        sounds.add(resolved);
                    }
                }
                definition.add("sounds", sounds);
                result.add(event, definition);
            }
            writeJson("assets/" + ns + "/sounds.json", result);
        }

        void languages() throws IOException {
            JsonObject baseEnglish = parse(base.readJar("assets/minecraft/lang/en_us.json")).getAsJsonObject();
            Map<String, File> extra = new TreeMap<>();
            for (File dir : getExtraLangDir().getFiles()) {
                File[] files = dir.isDirectory() ? dir.listFiles((d, n) -> n.endsWith(".json")) : new File[]{dir};
                if (files == null) continue;
                for (File f : files) extra.put(f.getName().replace(".json", ""), f);
            }
            Set<String> languages = new java.util.TreeSet<>(config.languages);
            languages.add("en_us");
            languages.addAll(extra.keySet());
            for (String lang : languages) {
                JsonObject translations = new JsonObject();
                byte[] data = lang.equals("en_us")
                        ? src.readJar("assets/minecraft/lang/en_us.json")
                        : (src.objectHash("minecraft/lang/" + lang + ".json") != null ? src.readObject("minecraft/lang/" + lang + ".json") : null);
                if (data != null) {
                    JsonObject vanilla = parse(data).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : new TreeMap<>(vanilla.asMap()).entrySet()) {
                        if (baseEnglish.has(e.getKey())) continue;
                        Matcher m = NAMESPACED_KEY.matcher(e.getKey());
                        String key = m.matches() ? m.group(1) + "." + ns + "." + m.group(2) : e.getKey();
                        translations.add(key, e.getValue());
                    }
                }
                File extraFile = extra.get(lang);
                if (extraFile != null) {
                    try (Reader reader = Files.newBufferedReader(extraFile.toPath(), StandardCharsets.UTF_8)) {
                        JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach(translations::add);
                    }
                }
                if (translations.size() > 0) writeJson("assets/" + ns + "/lang/" + lang + ".json", translations);
            }
        }

        // ------------------------------------------------------------ rewriting helpers

        private void rewriteModel(JsonObject model) {
            if (model.has("parent")) model.addProperty("parent", modelRef(model.get("parent").getAsString()));
            if (model.has("textures")) {
                JsonObject textures = model.getAsJsonObject("textures");
                for (String key : new ArrayList<>(textures.keySet())) {
                    JsonElement value = textures.get(key);
                    if (value.isJsonPrimitive() && !value.getAsString().startsWith("#")) {
                        textures.addProperty(key, textureRef(value.getAsString()));
                    }
                }
            }
            if (model.has("elements")) {
                for (JsonElement element : model.getAsJsonArray("elements")) {
                    rewriteElement(element.getAsJsonObject());
                }
            }
        }

        private void rewriteElement(JsonObject element) {
            if (element.has("rotation")) {
                JsonObject rotation = element.getAsJsonObject("rotation");
                if (!rotation.has("axis")) {
                    // Newer multi-axis rotations: keep the dominant axis only.
                    String axis = null;
                    double angle = 0;
                    for (String a : new String[]{"x", "y", "z"}) {
                        if (rotation.has(a) && Math.abs(rotation.get(a).getAsDouble()) > Math.abs(angle)) {
                            axis = a;
                            angle = rotation.get(a).getAsDouble();
                        }
                    }
                    if (axis == null) {
                        element.remove("rotation");
                        return;
                    }
                    rotation.remove("x");
                    rotation.remove("y");
                    rotation.remove("z");
                    rotation.addProperty("axis", axis);
                    rotation.addProperty("angle", angle);
                }
                double angle = rotation.get("angle").getAsDouble();
                double best = VALID_ANGLES[0];
                for (double candidate : VALID_ANGLES) {
                    if (Math.abs(candidate - angle) < Math.abs(best - angle)) best = candidate;
                }
                rotation.addProperty("angle", best);
            }
            if (element.has("light_emission")) {
                int light = element.get("light_emission").getAsInt();
                element.remove("light_emission");
                JsonObject data = new JsonObject();
                data.addProperty("block_light", light);
                data.addProperty("sky_light", light);
                element.add("neoforge_data", data);
            }
        }

        private void rewriteModelRefs(JsonElement element) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                for (String key : new ArrayList<>(object.keySet())) {
                    JsonElement value = object.get(key);
                    if (key.equals("model") && value.isJsonPrimitive()) {
                        object.addProperty(key, modelRef(value.getAsString()));
                    } else {
                        rewriteModelRefs(value);
                    }
                }
            } else if (element.isJsonArray()) {
                for (JsonElement child : element.getAsJsonArray()) rewriteModelRefs(child);
            }
        }

        private String modelRef(String ref) {
            String path = stripNamespace(ref);
            if (path.startsWith("builtin/")) return "minecraft:" + path;
            return (newModels.contains(path) ? ns : "minecraft") + ":" + path;
        }

        private String textureRef(String ref) {
            String path = stripNamespace(ref);
            return (newTextures.contains(path) ? ns : "minecraft") + ":" + path;
        }
    }

    // ---------------------------------------------------------------- vanilla data access

    private final class Assets {
        private final ZipFile jar;
        private final JsonObject objects;

        Assets(ZipFile jar, JsonObject index) {
            this.jar = jar;
            this.objects = index.getAsJsonObject("objects");
        }

        List<String> jarEntries(String prefix) {
            List<String> names = new ArrayList<>();
            jar.stream().map(ZipEntry::getName).filter(n -> n.startsWith(prefix) && !n.endsWith("/")).sorted().forEach(names::add);
            return names;
        }

        boolean hasJarEntry(String name) {
            return jar.getEntry(name) != null;
        }

        byte[] readJar(String name) throws IOException {
            try (InputStream in = jar.getInputStream(jar.getEntry(name))) {
                return in.readAllBytes();
            }
        }

        String objectHash(String name) {
            JsonObject object = objects.getAsJsonObject(name);
            return object == null ? null : object.get("hash").getAsString();
        }

        byte[] readObject(String name) throws IOException {
            String hash = objectHash(name);
            if (hash == null) throw new IOException("Asset " + name + " not found in asset index");
            String sub = hash.substring(0, 2) + "/" + hash;
            return Files.readAllBytes(download(RESOURCES_URL + sub, cache.resolve("objects/" + sub), hash, false));
        }
    }

    private record Version(Path jar, JsonObject index) {
    }

    private record LayoutConversion(String oldPattern, String newPattern, List<String> examples, List<String> targets) {
    }

    private Version resolveVersion(JsonObject manifest, String id) throws IOException {
        JsonObject entry = null;
        for (JsonElement v : manifest.getAsJsonArray("versions")) {
            if (v.getAsJsonObject().get("id").getAsString().equals(id)) entry = v.getAsJsonObject();
        }
        if (entry == null) throw new GradleException("Minecraft version " + id + " is not in Mojang's version manifest");
        String sha1 = entry.get("sha1").getAsString();
        JsonObject version = readJson(download(entry.get("url").getAsString(), cache.resolve("versions/" + id + ".json"), sha1, false));
        JsonObject client = version.getAsJsonObject("downloads").getAsJsonObject("client");
        Path jar = download(client.get("url").getAsString(), cache.resolve("versions/" + id + "-client.jar"), client.get("sha1").getAsString(), false);
        JsonObject assetIndex = version.getAsJsonObject("assetIndex");
        Path index = download(assetIndex.get("url").getAsString(), cache.resolve("indexes/" + assetIndex.get("id").getAsString() + ".json"),
                assetIndex.get("sha1").getAsString(), false);
        return new Version(jar, readJson(index));
    }

    private Path download(String url, Path target, String sha1, boolean refresh) throws IOException {
        if (!refresh && Files.exists(target) && (sha1 == null || sha1.equals(sha1(target)))) return target;
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        IOException last = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                if (sha1 != null && !sha1.equals(sha1(tmp))) throw new IOException("Checksum mismatch for " + url);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            } catch (IOException e) {
                last = e;
                if (refresh && Files.exists(target)) {
                    getLogger().warn("Could not refresh {} ({}), using the cached copy", url, e.toString());
                    return target;
                }
                try {
                    Thread.sleep(2000L << (attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new GradleException("Failed to download " + url + " - the build needs internet access once to fetch vanilla assets", last);
    }

    // ---------------------------------------------------------------- config

    private static final class Config {
        final Set<String> excludeBlocks = new HashSet<>();
        final Set<String> excludeItems = new HashSet<>();
        final List<String> excludeTexturePrefixes = new ArrayList<>();
        final List<String> excludeSoundPrefixes = new ArrayList<>();
        final Set<String> forceTextures = new HashSet<>();
        final List<String> languages = new ArrayList<>();
        final Map<String, String> copies = new LinkedHashMap<>();
        final Map<String, JsonObject> modelPatches = new LinkedHashMap<>();
        final List<LayoutConversion> layoutConversions = new ArrayList<>();

        static Config load(File file) throws IOException {
            Config config = new Config();
            if (file == null || !file.exists()) return config;
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            config.excludeBlocks.addAll(strings(json.get("exclude_blocks")));
            config.excludeItems.addAll(strings(json.get("exclude_items")));
            config.excludeTexturePrefixes.addAll(strings(json.get("exclude_texture_prefixes")));
            config.excludeSoundPrefixes.addAll(strings(json.get("exclude_sound_prefixes")));
            config.forceTextures.addAll(strings(json.get("force_textures")));
            config.languages.addAll(strings(json.get("languages")));
            if (json.has("patch_models")) {
                json.getAsJsonObject("patch_models").asMap().forEach((k, v) -> config.modelPatches.put(k, v.getAsJsonObject()));
            }
            if (json.has("layout_conversions")) {
                for (JsonElement e : json.getAsJsonArray("layout_conversions")) {
                    JsonObject o = e.getAsJsonObject();
                    config.layoutConversions.add(new LayoutConversion(o.get("old").getAsString(), o.get("new").getAsString(),
                            strings(o.get("examples")), strings(o.get("targets"))));
                }
            }
            if (json.has("copy")) {
                json.getAsJsonObject("copy").asMap().forEach((k, v) -> config.copies.put(k, v.getAsString()));
            }
            return config;
        }

        boolean isTextureExcluded(String rel) {
            return excludeTexturePrefixes.stream().anyMatch(rel::startsWith);
        }

        boolean isSoundExcluded(String event) {
            return excludeSoundPrefixes.stream().anyMatch(event::startsWith);
        }
    }

    // ---------------------------------------------------------------- utilities

    private Set<String> collectOverrides() throws IOException {
        Set<String> result = new HashSet<>();
        for (File root : getOverrideRoots().getFiles()) {
            if (!root.isDirectory()) continue;
            Path rootPath = root.toPath();
            try (Stream<Path> files = Files.walk(rootPath)) {
                files.filter(Files::isRegularFile).forEach(p -> result.add(rootPath.relativize(p).toString().replace(File.separatorChar, '/')));
            }
        }
        return result;
    }

    private void write(String path, byte[] data) throws IOException {
        if (overrides.contains(path)) return;
        Path target = out.resolve(path);
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        written++;
    }

    private void writeJson(String path, JsonElement json) throws IOException {
        write(path, (GSON.toJson(json) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> strings(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) result.add(e.getAsString());
        } else if (element instanceof JsonPrimitive) {
            result.add(element.getAsString());
        }
        return result;
    }

    private static String stripNamespace(String ref) {
        int colon = ref.indexOf(':');
        return colon >= 0 ? ref.substring(colon + 1) : ref;
    }

    private static String fileId(String entry) {
        String file = entry.substring(entry.lastIndexOf('/') + 1);
        return file.substring(0, file.lastIndexOf('.'));
    }

    private static JsonElement parse(byte[] data) {
        return JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
    }

    private static JsonObject readJson(Path path) throws IOException {
        return parse(Files.readAllBytes(path)).getAsJsonObject();
    }

    private static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
