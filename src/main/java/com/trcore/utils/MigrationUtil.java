package com.trcore.utils;

import java.io.File;
import java.util.logging.Logger;

public class MigrationUtil {

    public static void handleMigration(File pluginsFolder, Logger logger) {
        File oldFolder = new File(pluginsFolder, "vtCore");
        File newFolder = new File(pluginsFolder, "trCore");

        if (oldFolder.exists() && !newFolder.exists()) {
            logger.info("Eski veri klasörü (vtCore) bulundu. trCore olarak aktarılıyor...");
            if (oldFolder.renameTo(newFolder)) {
                logger.info("Aktarma işlemi başarıyla tamamlandı!");
            } else {
                logger.warning("Aktarma işlemi başarısız oldu! Lütfen dosyaları el ile taşıyın.");
            }
        }
    }
}
