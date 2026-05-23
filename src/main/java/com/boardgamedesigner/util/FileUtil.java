package com.boardgamedesigner.util;

import java.io.File;
import java.nio.file.Path;
import java.text.Collator;
import java.util.*;

/**
 * 文件筛选和排序工具.
 */
public final class FileUtil {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private FileUtil() {
    }

    /**
     * 扫描文件夹中的所有图片文件，按自然排序（支持中文）.
     *
     * @param folder 图片文件夹路径
     * @return 排序后的图片文件列表
     */
    public static List<Path> scanImageFiles(Path folder) {
        File[] files = folder.toFile().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<Path> imagePaths = Arrays.stream(files)
                .filter(File::isFile)
                .filter(f -> {
                    String ext = getExtension(f.getName()).toLowerCase(Locale.ROOT);
                    return IMAGE_EXTENSIONS.contains(ext);
                })
                .map(File::toPath)
                .toList();

        Collator collator = Collator.getInstance(Locale.CHINESE);
        List<Path> sorted = new ArrayList<>(imagePaths);
        sorted.sort((a, b) -> collator.compare(a.getFileName().toString(), b.getFileName().toString()));
        return sorted;
    }

    public static boolean isImageFile(Path path) {
        String ext = getExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.contains(ext);
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }
}
