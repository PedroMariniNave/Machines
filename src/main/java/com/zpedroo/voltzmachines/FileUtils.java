package com.zpedroo.voltzmachines;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class FileUtils {

    private static FileUtils instance;
    public static FileUtils get() { return instance; }

    private static String CHARSET_NAME = "UTF-8";

    private VoltzMachines voltzMachines;
    private Map<Files, FileManager> files;

    public FileUtils(VoltzMachines voltzMachines) {
        instance = this;
        this.voltzMachines = voltzMachines;
        this.files = new HashMap<>(Files.values().length);

        for (Files files : Files.values()) {
            getFiles().put(files, new FileManager(files));
        }
    }

    public String getString(Files file, String path) {
        return getString(file, path, "NULL");
    }

    public String getString(Files file, String path, String defaultValue) {
        return getFile(file).get().getString(path, defaultValue);
    }

    public List<String> getStringList(Files file, String path) {
        return getFiles().get(file).get().getStringList(path);
    }

    public Boolean getBoolean(Files file, String path) {
        return getFile(file).get().getBoolean(path);
    }

    public Integer getInt(Files file, String path) {
        return getInt(file, path, 0);
    }

    public Integer getInt(Files file, String path, int defaultValue) {
        return getFile(file).get().getInt(path, defaultValue);
    }

    public Long getLong(Files file, String path) {
        return getLong(file, path, 0);
    }

    public Long getLong(Files file, String path, long defaultValue) {
        return getFile(file).get().getLong(path, defaultValue);
    }

    public Double getDouble(Files file, String path) {
        return getDouble(file, path, 0);
    }

    public Double getDouble(Files file, String path, double defaultValue) {
        return getFile(file).get().getDouble(path, defaultValue);
    }

    public Float getFloat(Files file, String path) {
        return getFloat(file, path, 0);
    }

    public Float getFloat(Files file, String path, float defaultValue) {
        return (float) getFile(file).get().getDouble(path, defaultValue);
    }

    public Set<String> getSection(Files file, String path) {
        return getFile(file).get().getConfigurationSection(path).getKeys(false);
    }

    public FileManager getFile(Files file) {
        return getFiles().get(file);
    }

    public Map<Files, FileManager> getFiles() {
        return files;
    }

    private void copy(InputStream is, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;

            while ((len=is.read(buf)) > 0) {
                out.write(buf,0,len);
            }

            out.close();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public enum Files {
        CONFIG("config", "yml", "configuration-files", "", false),
        MANAGERS("managers", "yml", "menus", "menus", false),
        PERMISSIONS("permissions", "yml", "menus", "menus", false),
        PLAYER_MACHINES("player_machines", "yml", "menus", "menus", false),
        OTHER_MACHINES("other_machines", "yml", "menus", "menus", false),
        TOP_MACHINES("top_machines", "yml", "menus", "menus", false),
        SHOP("shop", "yml", "menus", "menus", false),
        GIFT("gift", "yml", "menus", "menus", false),
        MAIN("main", "yml", "menus", "menus", false),
        EMERALD("emerald", "yml", "machines", "machines", true);

        public String name;
        public String extension;
        public String resource;
        public String folder;
        public Boolean requireEmpty;

        Files(String name, String extension, String resource, String folder, Boolean requireEmpty) {
            this.name = name;
            this.extension = extension;
            this.resource = resource;
            this.folder = folder;
            this.requireEmpty = requireEmpty;
        }

        public String getName() {
            return name;
        }

        public String getExtension() {
            return extension;
        }

        public String getResource() {
            return resource;
        }

        public String getFolder() {
            return folder;
        }
    }

    public class FileManager {

        private File file;
        private FileConfiguration yamlConfig;

        public FileManager(Files file) {
            this.file = new File(voltzMachines.getDataFolder() + (file.getFolder().isEmpty() ? "" : "/" + file.getFolder()), file.getName() + '.' + file.getExtension());

            if (!this.file.exists()) {
                if (file.requireEmpty) {
                    File folder = new File(voltzMachines.getDataFolder(), "/machines");
                    if (folder.listFiles() != null) {
                        if (Stream.of(folder.listFiles()).map(YamlConfiguration::loadConfiguration).count() > 0) return;
                    }
                }

                try {
                    this.file.getParentFile().mkdirs();
                    this.file.createNewFile();

                    copy(voltzMachines.getResource((file.getResource().isEmpty() ? "" : file.getResource() + "/") + file.getName() + '.' + file.getExtension()), this.file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (!StringUtils.equals(file.getExtension(), "yml")) return;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), CHARSET_NAME));
                yamlConfig = YamlConfiguration.loadConfiguration(reader);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public FileConfiguration get() {
            return yamlConfig;
        }

        public File getFile() {
            return file;
        }

        public void save() {
            try {
                yamlConfig.save(file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void reload() {
            try {
                yamlConfig = YamlConfiguration.loadConfiguration(file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}