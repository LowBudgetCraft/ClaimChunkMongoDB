package de.glowman554.claimchunkmongodb;

import de.glowman554.config.ConfigFile;
import de.glowman554.config.auto.Saved;

import java.io.File;

public class Configuration extends ConfigFile {
    @Saved
    public String uri = "";
    @Saved
    public String database = "chunkclaim";

    public Configuration(File configFile) {
        super(configFile);
    }
}
