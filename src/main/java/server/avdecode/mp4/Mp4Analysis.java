package server.avdecode.mp4;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * mp4视频文件解析
 * Created by Dell on 8/11/2017.
 *
 * @author yyz
 */
public class Mp4Analysis {
    private byte[] avcC = new byte[]{0x61, 0x76, 0x63, 0x43};
    public static byte[] mdat = new byte[]{0x6d, 0x64, 0x61, 0x74};
    private byte[] tag = new byte[]{0x30, 0x30, 0x00, (byte) 0x80};

    private static byte[] SPS = null;
    private static byte[] PPS = null;
    /**
     * mdat在文件中的开始位置和结束位置
     */
    private int[] mdatIndex = null;

    //每个chunk的偏移
    private int[] arrayStco = null;
    //每个sample的大小
    private int[] arrayStsz = null;
    //sample和chunk的映射表
    private int[] arrayStsc = null;
    //关键帧列表
    private int[] arrayStss = null;


    private long index = 0;
    private RandomAccessFile file = null;
    /**
     * MP4文件中的所有box
     */
    private Map<String, Box> boxesMap = null;


    public void release() {
        SPS = null;
        PPS = null;
        mdatIndex = null;
        arrayStco = null;
        arrayStsz = null;
        arrayStsc = null;
        if (boxesMap != null) {
            boxesMap.clear();
            boxesMap = null;
        }
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            file = null;
        }
    }

    /**
     * 解析MP4文件中的所有box
     *
     * @param path MP4文件的路径
     * @return 返回所有box的开始地址与结束地址
     * @throws Exception
     */
    public void init(String path) throws Exception {
        file = new RandomAccessFile(new File(path), "r");
        boxesMap = new HashMap<>();
        parseFile(file, boxesMap, file.length());
    }

//    public Map<String, Box> getBoxesMap()
//    {
//        return boxesMap;
//    }

    public void parseFile(RandomAccessFile file, Map<String, Box> boxesMap, long length) throws IOException {
        long sum = 0;
        byte[] buffer = new byte[8];

        while (sum < length || index < length) {
            int ret = file.read(buffer);
            if (ret < 1) {
                break;
            }
            sum += buffer.length;
            index += buffer.length;

            if (validBoxName(buffer)) {
                if (buffer[3] == 1) {
                    // 64 bits atom size
                    file.read(buffer);
                    sum += 8;
                    index += buffer.length;

                    String name = new String(buffer, 4, 4);
                    long newLen = byteToInt(buffer, 0);
                    Box box = new Box();
                    box.startIndex = sum;
                    box.endIndex = newLen;
                    boxesMap.put(name, box);
                    file.skipBytes((int) (newLen - sum));
                } else {
                    // 32 bits atom size
                    long newLen = byteToInt(buffer, 0);
                    String name = new String(buffer, 4, 4);

                    Box box = new Box();
                    box.startIndex = index;
                    box.endIndex = index + newLen - 8;
                    if (boxesMap.containsKey(name)) {
                        Box tmp = boxesMap.get(name);
                        boxesMap.remove(name);
                        String key = "video_" + name;
                        boxesMap.put(key, tmp);
                        name = "sound_" + name;
                    }
                    boxesMap.put(name, box);
//                    Logcat.d("Atom -> name: " + name + " startPosition: " + box.startIndex + ", endPosition: " + box.endIndex);
                    if ("mdat".equals(name)) {
                        long skip = box.endIndex - box.startIndex;
                        file.skipBytes((int) skip);
                        sum += skip;
                        index += skip;
                    } else {
                        long size = box.endIndex - box.startIndex;
                        parseFile(file, boxesMap, size);
                    }
                }
            } else {
                long skip = length - sum;
                file.skipBytes((int) (skip));
                index += skip;
                break;
            }
        }
    }

    /**
     * 判断给buffer是否包含了box
     *
     * @param buffer
     * @return 包含返回true
     */
    private boolean validBoxName(byte[] buffer) {
        for (int i = 0; i < 4; i++) {
            boolean existed = (buffer[i + 4] < 'a' || buffer[i + 4] > 'z') && (buffer[i + 4] < '0' || buffer[i + 4] > '9');
            // If the next 4 bytes are neither lowercase letters nor numbers
            if (existed) {
                return false;
            }
        }
        return true;
    }

    public static class Stsc {
        /**
         * stco的偏移地址
         */
        public int firstChunk = 0;
        public int SamplesPerChunk = 0;
        public int SampleDescriptionIndex = 0;
    }

    public static class Box {
        public long startIndex = 0;
        public long endIndex = 0;
    }

    public boolean findAvcC(byte[] data) {
        // avcC的起始位置
        int avcCRecord = 0;

        for (int index = 0; index < data.length; index++) {
            if (data[index] != avcC[0]) {
                continue;
            }
            if (data[index + 1] == avcC[1] && data[index + 2] == avcC[2] && data[index + 3] == avcC[3]) {
                // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                avcCRecord = index + 4;
                break;
            }
        }
        if (0 == avcCRecord) {
            return false;
        }

        int spsStartPos = avcCRecord + 6;
        int spsLength = byteToInt(data, spsStartPos, spsStartPos + 1);
        SPS = new byte[spsLength];
        spsStartPos += 2;
        System.arraycopy(data, spsStartPos, SPS, 0, spsLength);

        int ppsStartPos = spsStartPos + spsLength + 1;
        int ppsLength = byteToInt(data, ppsStartPos, ppsStartPos + 1);
        PPS = new byte[ppsLength];
        ppsStartPos += 2;
        System.arraycopy(data, ppsStartPos, PPS, 0, ppsLength);

        return avcCRecord > 0;
    }

    public boolean findMdatAvcC(byte[] data) {
        // avcC的起始位置
        int avcCRecord = 0;
//        int mdatRecord = 0;

        for (int index = 0; index < data.length; index++) {
            if (data[index] != avcC[0] && data[index] != mdat[0]) {
                continue;
            }
            if (data[index + 1] == avcC[1] && data[index + 2] == avcC[2] && data[index + 3] == avcC[3]) {
                // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                avcCRecord = index + 4;
                if (mdatIndex != null) {
                    break;
                }
            } else if (data[index + 1] == mdat[1] && data[index + 2] == mdat[2] && data[index + 3] == mdat[3]) {
                // 找到mdat，则记录mdatRecord起始位置，然后退出循环。
                mdatIndex = new int[2];
                int mdatLength = byteToInt(data, index - 4);
                mdatIndex[1] = mdatLength + index - 4;
                int tmp = index + 4;
                if (data[tmp + 4] != 0x65) {
                    //mdat后面还不是真正的内容
                    int skip = byteToInt(data, tmp);
//                    int skip = byteToInt(data, tmp, tmp + 3);
                    skip += tmp;
                    if (data[skip] != 0x65) {
                        if (data[skip] == tag[0] && data[skip + 1] == tag[1] && data[skip + 2] == tag[2]
                                && data[skip + 3] == tag[3] && data[skip + 8] == 0x65) {
                            //找到
                            mdatIndex[0] = skip + 4;
                        }
                        if (avcCRecord > 0) {
                            break;
                        }
                        continue;
                    } else {
                        mdatIndex[0] = skip + 1;
                    }
                } else {
                    mdatIndex[0] = tmp;
                }
                if (avcCRecord > 0) {
                    break;
                }
            }
        }
        if (0 == avcCRecord && mdatIndex == null) {
            return false;
        }

        if (avcCRecord > 0) {
            int spsStartPos = avcCRecord + 6;
            int spsLength = byteToInt(data, spsStartPos, spsStartPos + 1);
            SPS = new byte[spsLength];
            spsStartPos += 2;
            System.arraycopy(data, spsStartPos, SPS, 0, spsLength);

            int ppsStartPos = spsStartPos + spsLength + 1;
            int ppsLength = byteToInt(data, ppsStartPos, ppsStartPos + 1);
            PPS = new byte[ppsLength];
            ppsStartPos += 2;
            System.arraycopy(data, ppsStartPos, PPS, 0, ppsLength);
        }

        return avcCRecord > 0 || mdatIndex != null;
    }


    private int byteToInt(byte[] bt, int start, int end) {
        int ret = bt[start];
        ret <<= 8;
        ret |= bt[end];
        return ret;
    }

    public static int byteToInt(byte[] src, int offset) {
        int value;
        value = (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

//    public static int byteToInt(byte[] src, int offset)
//    {
//        int value = src[offset];
//        for (int index = ++offset; index < 3; ++index)
//        {
//            value <<= 8;
//            value |= (src[index] & 0xff);
//        }
//        return value;
//    }

    public static long byteToLong(byte[] byteNum) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    /**
     * @throws Exception
     */
    public void findVideoBox() throws Exception {
        try {
            //查找stss
//            arrayStss = findVideoStss(boxesMap,file);
            //查找stco
            arrayStco = findVideoStco(boxesMap, file);
            //查找stsz
            arrayStsz = findVideoStsz(boxesMap, file);
            //查找stsc
            arrayStsc = findVideoStsc(boxesMap, file);
            //查找avcC
            findAvcC(boxesMap, file);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        } finally {
            file.close();
        }
    }

    /**
     * 查找指定box名称中的内容
     *
     * @param boxesMap 扫描出视频文件的所有box集合
     * @param file     视频文件
     * @param boxName  box名称（视频以video_开头,音频以audio_开头）
     * @return
     * @throws Exception
     */
    public byte[] findBox(Map<String, Box> boxesMap, RandomAccessFile file, String boxName) throws Exception {
        Box box = boxesMap.get(boxName);
        if (box == null) {
            String[] array = boxName.split("_");
            box = boxesMap.get(array[1]);
        }
        file.seek(box.startIndex);
        byte[] buffer = new byte[(int) (box.endIndex - box.startIndex)];
        file.read(buffer);
        return buffer;
    }

    /**
     * 支持查找 stts ctts stss sdtp arrayStsc arrayStsz arrayStco
     *
     * @param boxesMap 扫描出视频文件的所有box集合
     * @param file     视频文件
     * @param boxName  box名称（视频以video_开头,音频以audio_开头）
     * @return
     * @throws Exception
     */
    public int[] findBoxStbl(Map<String, Box> boxesMap, RandomAccessFile file, String boxName) throws Exception {
        byte[] data = findBox(boxesMap, file, boxName);
        //头部是stsz的数量所以要加4
        int skip = boxName.contains("video_stsz") ? 12 : 8;

        int[] ret = new int[(data.length - skip) / 4];
        int count = 0;
        for (int index = skip; index < data.length; index += 4) {
            ret[count] = byteToInt(data, index);
            count++;
        }
        return ret;
    }

    public int[] findVideoStsc(Map<String, Box> boxesMap, String path) throws Exception {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        int[] ret = findVideoStsc(boxesMap, file);
        file.close();
        return ret;
    }

    public int[] findVideoStsc(Map<String, Box> boxesMap, RandomAccessFile file) throws Exception {
        return findBoxStbl(boxesMap, file, "video_stsc");
    }

    public int[] findVideoStsz(Map<String, Box> boxesMap, String path) throws Exception {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        int[] ret = findVideoStsz(boxesMap, file);
        file.close();
        return ret;
    }

    public int[] findVideoStsz(Map<String, Box> boxesMap, RandomAccessFile file) throws Exception {
        return findBoxStbl(boxesMap, file, "video_stsz");
    }

    public int[] findVideoStco(Map<String, Box> boxesMap, String path) throws Exception {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        int[] ret = findVideoStco(boxesMap, file);
        file.close();
        return ret;
    }

    public int[] findVideoStco(Map<String, Box> boxesMap, RandomAccessFile file) throws Exception {
        return findBoxStbl(boxesMap, file, "video_stco");
    }

    public int[] findVideoStss(Map<String, Box> boxesMap, String path) throws Exception {
        RandomAccessFile file = new RandomAccessFile(path, "r");
        int[] ret = findVideoStss(boxesMap, file);
        file.close();
        return ret;
    }

    public int[] findVideoStss(Map<String, Box> boxesMap, RandomAccessFile file) throws Exception {
        return findBoxStbl(boxesMap, file, "video_stss");
    }

    public void findAvcC() throws Exception {
        findAvcC(boxesMap, file);
    }

    /**
     * 查找avcC
     *
     * @param boxesMap
     * @param file
     * @throws Exception
     */
    public void findAvcC(Map<String, Box> boxesMap, RandomAccessFile file) throws Exception {

        byte[] stsd = findBox(boxesMap, file, "video_stsd");
        findAvcC(stsd);
    }

    /*=====================================================================================*/
    /*                    下面方法是获取分析结果                                           */
    /*=====================================================================================*/

    public byte[] getSPS() {
        return SPS;
    }

    public byte[] getPPS() {
        return PPS;
    }

    public static String getBase64SPS(byte[] sps) {
        if (SPS == null && sps == null) {
            return null;
        }
        byte[] tmp = sps == null ? SPS : sps;
        return Base64.getEncoder().encodeToString(tmp);
//        return Base64.encodeToString(tmp, Base64.NO_WRAP);
    }

    public static String getBase64PPS(byte[] pps) {
        if (PPS == null && pps == null) {
            return null;
        }
        byte[] tmp = pps == null ? PPS : pps;
        return Base64.getEncoder().encodeToString(tmp);
//        return Base64.encodeToString(tmp, Base64.NO_WRAP);
    }

    public static byte[] getSPS(String base64SPS) {
        if (base64SPS == null) {
            return null;
        }
        return Base64.getDecoder().decode(base64SPS);
//        return Base64.decode(base64SPS, Base64.NO_WRAP);
    }

    public static byte[] getPPS(String base64PPS) {
        if (base64PPS == null) {
            return null;
        }
        return Base64.getDecoder().decode(base64PPS);
//        return Base64.decode(base64PPS, Base64.NO_WRAP);
    }

    public int[] getMdatIndex() {
        return mdatIndex;
    }

    public static String getProfileLevel(byte[] sps) {
        if (sps == null && SPS == null) {
            return null;
        }
        byte[] tmp = sps == null ? SPS : sps;
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < 4; i++) {
            String str = Integer.toHexString(tmp[i] & 0xFF);
            if (str.length() < 2) {
                builder.append("0");
            }
            builder.append(str);
        }
        return builder.toString();
    }

    public int[] getStco() {
        return arrayStco;
    }

    public int[] getStsz() {
        return arrayStsz;
    }

    public int[] getStsc() {
        return arrayStsc;
    }

    public List<Stsc> getListStsc() {
        List<Stsc> stscList = null;
        if (arrayStsc != null) {
            stscList = new ArrayList<>(arrayStsc.length / 3);
            for (int index = 0; index < arrayStsc.length;) {
                Stsc stsc = new Stsc();
                stsc.firstChunk = arrayStsc[index++];
                stsc.SamplesPerChunk = arrayStsc[index++];
                stsc.SampleDescriptionIndex = arrayStsc[index++];
                stscList.add(stsc);
            }
        }
        return stscList;
    }
}
