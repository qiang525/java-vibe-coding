package com.nnuist.coursehub.common.util;

import com.nnuist.coursehub.common.constant.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 文件处理工具类
 *
 * <p>提供四类能力：<b>算哈希、取扩展名、生成存储路径、校验文件头</b>。</p>
 *
 * @author CourseHub Team
 */
public final class FileUtil {

    private FileUtil() {
        // 工具类不允许实例化
    }

    /** 按日期分层存放，避免单个目录塞几万个文件（文件系统会变慢） */
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 读文件时的缓冲区大小：8KB */
    private static final int BUFFER_SIZE = 8192;

    // =========================================================================
    //  一、MD5 哈希
    // =========================================================================

    /**
     * 计算文件的 MD5（32 位小写十六进制）
     *
     * <p><b>MD5 在这里不是用来"加密"的</b>（它不可逆，也早就被证明不安全），
     * 而是用来做<b>文件指纹</b>——同一份文件算出来的 MD5 必然相同，
     * 内容差一个字节结果就完全不同。</p>
     *
     * <p>用途：秒传（判断这个文件是不是已经有人传过）。</p>
     *
     * <p><b>为什么用流式读取而不是一次读进内存</b>：
     * 500MB 的课件如果整个读进内存，几个用户同时上传就会把服务撑爆。
     * 流式读取每次只占用 8KB 内存，无论文件多大都一样。</p>
     */
    public static String md5(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return md5(in);
        }
    }

    /**
     * 计算流的 MD5
     *
     * <p><b>注意</b>：这个方法会读完整个流，调用后流就到底了，
     * 需要重新读取的话得重新打开一个流。</p>
     */
    public static String md5(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 算法不可用", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(out);
    }

    // =========================================================================
    //  二、扩展名
    // =========================================================================

    /**
     * 取扩展名（小写，不含点）
     *
     * <p>{@code "报告.PDF" -> "pdf"}；没有扩展名则返回空串。</p>
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 扩展名是否在允许名单里
     *
     * <p><b>为什么用"白名单"而不是"黑名单"</b>：
     * 黑名单（禁止 .exe/.bat/.sh）永远列不全 —— 攻击者可以用 .jsp、.py、
     * 或者完全没见过的后缀。白名单（只允许 .pdf/.docx/...）是"默认拒绝"，
     * 天然更安全。</p>
     */
    public static boolean isAllowedExtension(String ext) {
        if (ext == null || ext.isEmpty()) {
            return false;
        }
        for (String allowed : Constants.ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    //  三、存储路径
    // =========================================================================

    /** 今天的日期目录，如 {@code 2026/10/09} */
    public static String datePath() {
        return LocalDate.now().format(DATE_DIR);
    }

    /**
     * 生成随机存储文件名
     *
     * <p><b>为什么必须重命名，不能用用户传的原始文件名</b>：</p>
     * <ol>
     *   <li>原始文件名可能是 {@code ../../evil.sh}（路径穿越）</li>
     *   <li>可能包含中文、空格、特殊符号，在不同系统上行为不一致</li>
     *   <li>两个人传同名文件会互相覆盖</li>
     * </ol>
     * <p>用 UUID 重命名后，磁盘上的名字和应用层完全解耦。
     * 用户看到的原始文件名存在数据库 {@code file.file_name} 字段里，
     * 下载时再还原回去 —— <b>存的和显示的分开</b>。</p>
     */
    public static String randomName(String ext) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (ext == null || ext.isEmpty()) ? uuid : uuid + "." + ext;
    }

    /** 生成完整的相对存储路径，如 {@code 2026/10/09/a1b2c3....pdf} */
    public static String buildRelativePath(String ext) {
        return datePath() + "/" + randomName(ext);
    }

    // =========================================================================
    //  四、文件头（魔术数字）校验
    // =========================================================================

    /**
     * 扩展名 → 允许的文件头字节
     *
     * <p><b>为什么光校验扩展名不够</b>：扩展名只是文件名的后缀，
     * 可以随便改。攻击者把木马改成 {@code 课件.pdf} 就能绕过扩展名检查。
     * 而"文件头"是文件内容的前几个字节，是<b>文件格式本身决定的，改不了</b>。
     * 两者结合才能确认"这确实是个 PDF，而不是伪装成 PDF 的东西"。</p>
     */
    private static final Map<String, List<byte[]>> MAGIC_NUMBERS = new HashMap<>();

    static {
        // Office 2007+ 系列的 docx/xlsx/pptx 本质都是 zip 包
        byte[] zipMagic = {0x50, 0x4B, 0x03, 0x04};
        // Office 97-2003 系列是 OLE 复合文档
        byte[] oleMagic = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

        MAGIC_NUMBERS.put("pdf", Collections.singletonList(new byte[]{0x25, 0x50, 0x44, 0x46}));   // %PDF
        MAGIC_NUMBERS.put("png", Collections.singletonList(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}));
        MAGIC_NUMBERS.put("jpg", Collections.singletonList(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        MAGIC_NUMBERS.put("jpeg", Collections.singletonList(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        MAGIC_NUMBERS.put("gif", Collections.singletonList(new byte[]{0x47, 0x49, 0x46, 0x38}));   // GIF8
        MAGIC_NUMBERS.put("zip", Collections.singletonList(zipMagic));
        MAGIC_NUMBERS.put("docx", Collections.singletonList(zipMagic));
        MAGIC_NUMBERS.put("xlsx", Collections.singletonList(zipMagic));
        MAGIC_NUMBERS.put("pptx", Collections.singletonList(zipMagic));
        MAGIC_NUMBERS.put("doc", Collections.singletonList(oleMagic));
        MAGIC_NUMBERS.put("xls", Collections.singletonList(oleMagic));
        MAGIC_NUMBERS.put("ppt", Collections.singletonList(oleMagic));
        MAGIC_NUMBERS.put("rar", Collections.singletonList(new byte[]{0x52, 0x61, 0x72, 0x21}));   // Rar!
        MAGIC_NUMBERS.put("7z", Collections.singletonList(new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C}));

        // 说明：txt / md 是纯文本，没有固定文件头，因此不登记，跳过校验
    }

    /**
     * 该扩展名是否需要做文件头校验
     */
    public static boolean needsMagicCheck(String ext) {
        return ext != null && MAGIC_NUMBERS.containsKey(ext.toLowerCase(Locale.ROOT));
    }

    /**
     * 校验文件头是否与扩展名匹配
     *
     * @return true 表示匹配（或该类型无需校验）
     */
    public static boolean matchesMagicNumber(Path path, String ext) throws IOException {
        List<byte[]> candidates = MAGIC_NUMBERS.get(ext == null ? "" : ext.toLowerCase(Locale.ROOT));
        if (candidates == null) {
            return true; // 没登记的类型，不做严格校验
        }
        byte[] head = new byte[8];
        try (InputStream in = Files.newInputStream(path)) {
            int read = in.read(head);
            if (read < 2) {
                return false; // 空文件或不足 2 字节，肯定不是正常文档
            }
        }
        for (byte[] magic : candidates) {
            if (startsWith(head, magic)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
