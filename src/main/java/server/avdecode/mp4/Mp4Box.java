package server.avdecode.mp4;


import util.LogDog;
import util.StringUtils;
import util.TypeConversion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Mp4Box {

    private FtypBox ftypBox = null;
    private MoovBox moovBox = null;
    private TrakBox currentTrakBox = null;

    private RandomAccessFile mp4File;
    private byte[] tag = new byte[8];

    public Mp4Box(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            throw new IllegalArgumentException("filePath is illegal ,please check it again!");
        }
        try {
            mp4File = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("filePath is illegal ,please check it again!");
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
            // If the next 4 bytes are neither lowercase letters nor numbers
            if ((buffer[i + 4] < 'a' || buffer[i + 4] > 'z') && (buffer[i + 4] < '0' || buffer[i + 4] > '9'))
                return false;
        }
        return true;
    }


    /**
     * 分析MP4文件的所有box信息
     *
     * @throws IOException
     */
    public void analysis() throws Exception {
        mp4File.seek(0);
        while (mp4File.getFilePointer() < mp4File.length()) {
            int ret = mp4File.read(tag);
            if (ret < 1) {
                throw new IOException("file is end or stream has error  !");
            }
            if (validBoxName(tag)) {
                if (tag[3] == 1) {
                    // 64 bits atom size
                } else {
                    // 32 bits atom size
                    long length = TypeConversion.byteToInt(tag, 0);
                    String name = new String(tag, 4, 4);
                    LogDog.d("==> boxName = " + name + " length = " + length);
                    saveBox(name, length);
                }
            }
        }
    }


    private String transForLongToDate(Long millSec) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(millSec);
        return sdf.format(date);
    }

    private void saveBox(String name, long length) throws Exception {
        switch (name) {
            case FtypBox.BOX_NAME:
                ftypBox = new FtypBox(length);
                ftypBox.parseSub();
                break;
            case MoovBox.BOX_NAME:
                moovBox = new MoovBox(length);
                moovBox.parseSub();
                break;
            case MvhdBox.BOX_NAME:
                MvhdBox mvhdBox = new MvhdBox(length);
                moovBox.mvhdBox = mvhdBox;
                mvhdBox.parseSub();
                break;
            case TrakBox.BOX_NAME:
                currentTrakBox = new TrakBox(length);
                moovBox.trakBoxes.add(currentTrakBox);
                break;
            case TkhdBox.BOX_NAME:
                if (currentTrakBox != null) {
                    TkhdBox tkhdBox = new TkhdBox(currentTrakBox, length);
                    currentTrakBox.tkhdBox = tkhdBox;
                    tkhdBox.parseSub();
                }
                break;
            case EdtsBox.BOX_NAME:
                if (currentTrakBox != null) {
                    EdtsBox edtsBox = new EdtsBox(length);
                    currentTrakBox.edtsBox = edtsBox;
                    edtsBox.parseSub();
                }
                break;
            case ElstBox.BOX_NAME:
                if (currentTrakBox != null) {
                    ElstBox elstBox = new ElstBox(length);
                    currentTrakBox.edtsBox.elstBox = elstBox;
                    elstBox.parseSub();
                }
                break;
            case MdiaBox.BOX_NAME:
                if (currentTrakBox != null) {
                    MdiaBox mdiaBox = new MdiaBox(length);
                    currentTrakBox.mdiaBox = mdiaBox;
                    mdiaBox.parseSub();
                }
                break;
            case MdhdBox.BOX_NAME:
                if (currentTrakBox != null) {
                    MdhdBox mdhdBox = new MdhdBox(length);
                    currentTrakBox.mdiaBox.mdhdBox = mdhdBox;
                    mdhdBox.parseSub();
                }
                break;
            case HdlrBox.BOX_NAME:
                if (currentTrakBox != null) {
                    HdlrBox hdlrBox = new HdlrBox(length);
                    currentTrakBox.mdiaBox.hdlrBox = hdlrBox;
                    hdlrBox.parseSub();
                }
                break;
            case MinfBox.BOX_NAME:
                if (currentTrakBox != null) {
                    MinfBox minfBox = new MinfBox(length);
                    currentTrakBox.mdiaBox.minfBox = minfBox;
                    minfBox.parseSub();
                }
                break;
            case VmhdBox.BOX_NAME:
                if (currentTrakBox != null) {
                    VmhdBox vmhdBox = new VmhdBox(length);
                    currentTrakBox.mdiaBox.minfBox.vmhdBox = vmhdBox;
                    vmhdBox.parseSub();
                }
                break;
            case DinfBox.BOX_NAME:
                if (currentTrakBox != null) {
                    DinfBox dinfBox = new DinfBox(length);
                    currentTrakBox.mdiaBox.minfBox.dinfBox = dinfBox;
                    dinfBox.parseSub();
                }
                break;
            case DrefBox.BOX_NAME:
                if (currentTrakBox != null) {
                    DrefBox drefBox = new DrefBox(length);
                    currentTrakBox.mdiaBox.minfBox.dinfBox.drefBox = drefBox;
                    drefBox.parseSub();
                }
                break;
            case StblBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StblBox stblBox = new StblBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox = stblBox;
                    stblBox.parseSub();
                }
                break;
            case StsdBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StsdBox stsdBox = new StsdBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stsdBox = stsdBox;
                    stsdBox.parseSub();
                }
                break;
            case SttsBox.BOX_NAME:
                if (currentTrakBox != null) {
                    SttsBox sttsBox = new SttsBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.sttsBox = sttsBox;
                    sttsBox.parseSub();
                }
                break;
            case CttsBox.BOX_NAME:
                if (currentTrakBox != null) {
                    CttsBox cttsBox = new CttsBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.cttsBox = cttsBox;
                    cttsBox.parseSub();
                }
                break;
            case StssBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StssBox stssBox = new StssBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stssBox = stssBox;
                    stssBox.parseSub();
                }
                break;
            case StscBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StscBox stscBox = new StscBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stscBox = stscBox;
                    stscBox.parseSub();
                }
                break;
            case StszBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StszBox stszBox = new StszBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stszBox = stszBox;
                    stszBox.parseSub();
                }
                break;
            case StcoBox.BOX_NAME:
                if (currentTrakBox != null) {
                    StcoBox stcoBox = new StcoBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stcoBox = stcoBox;
                    stcoBox.parseSub();
                }
                break;
            default:
                skipBox(length - 8);
                break;
        }
    }

    private void skipBox(long length) throws IOException {
        //当前的box不对应,跳过这个box
        if (length > Integer.MAX_VALUE) {
            while (length > 0) {
                int tmp = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (length - tag.length);
                mp4File.skipBytes(tmp);
                length -= tmp;
            }
        } else {
            mp4File.skipBytes((int) length - tag.length);
        }
    }


    public FtypBox getFtypBox() {
        return ftypBox;
    }

    public MoovBox getMoovBox() {
        return moovBox;
    }

    public void release() {
        moovBox.release();
        ftypBox.release();
        tag = null;
        if (mp4File != null) {
            try {
                mp4File.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mp4File = null;
        }
    }

    private abstract class Box {
        protected byte[] srcData;

        public Box(boolean isHasData, long length) throws IOException {
            if (isHasData) {
                srcData = new byte[(int) length - tag.length];
                mp4File.read(srcData);
                LogDog.i("==> srcData " + getClass().getName() + " = " + StringUtils.byteToHexStr(srcData));
            }
        }

        protected int[] parseArray() {
            int[] array = new int[srcData.length / 4];
            int count = 0;
            for (int index = 0; index < srcData.length; index += 4) {
                array[count] = TypeConversion.byteToInt(srcData, index);
                count++;
            }
            return array;
        }

        protected abstract void parseSub() throws Exception;

        public void release() {
            srcData = null;
        }
    }

    /*-------------------------------下面是所有Box------------------------------*/

//    public class AvcCBox extends Box {
//        public final static String BOX_NAME = "avcC";
//
//        public AvcCBox(long length) throws IOException {
//            super(true, length);
//        }
//
//        @Override
//        protected void parseSub() throws Exception {
//
//        }
//    }


    public class FtypBox extends Box {
        public final static String BOX_NAME = "ftyp";
        public String majorBrand = null;
        public String minorVersion = null;
        public String compatibleBrands = null;

        public FtypBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            int index = 0;
            majorBrand = new String(srcData, index, 4);
            index += 4;
            minorVersion = String.valueOf(TypeConversion.byteToInt(srcData, index));
            index += 4;
            compatibleBrands = new String(srcData, index, srcData.length - index);
            LogDog.d("==> majorBrand = " + majorBrand);
            LogDog.d("==> minorVersion = " + minorVersion);
            LogDog.d("==> compatibleBrands = " + compatibleBrands);
            srcData = null;
        }

        @Override
        public void release() {
            super.release();
            majorBrand = null;
            minorVersion = null;
            compatibleBrands = null;
        }
    }

    public class MoovBox extends Box {
        public final static String BOX_NAME = "moov";
        public List<TrakBox> trakBoxes;
        public MvhdBox mvhdBox = null;

        public MoovBox(long length) throws IOException {
            super(false, length);
            trakBoxes = new ArrayList<>();
        }


        @Override
        protected void parseSub() throws Exception {
        }

        @Override
        public void release() {
            super.release();
            if (mvhdBox != null) {
                mvhdBox.release();
                mvhdBox = null;
            }
            if (trakBoxes != null) {
                for (TrakBox box : trakBoxes) {
                    box.release();
                }
                trakBoxes.clear();
                trakBoxes = null;
            }
        }
    }

    public class MvhdBox extends Box {
        public final static String BOX_NAME = "mvhd";
        public String creationTime = null;
        public String modificationTime = null;
        public int timeScale = 0;
        public int duration = 0;
        public int durationMs = 0;
        public int rate = 0;
        public int volume = 0;
        public int nextTrackId = 0;

        public MvhdBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            long time = TypeConversion.byteToLong(srcData);
            creationTime = transForLongToDate(time);
            long mtime = TypeConversion.byteToLong(srcData, 8, 12);
            modificationTime = transForLongToDate(mtime);
            timeScale = TypeConversion.byteToInt(srcData, 12);
            duration = TypeConversion.byteToInt(srcData, 16);
            nextTrackId = TypeConversion.byteToInt(srcData, srcData.length - 4);
        }
    }

    public class TrakBox extends Box {
        public final static String BOX_NAME = "trak";
        public TkhdBox tkhdBox = null;
        public EdtsBox edtsBox = null;
        public MdiaBox mdiaBox = null;
        public boolean isMovie = false;

        public TrakBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
//            tkhdBox = new TkhdBox();
//            tkhdBox.parseSub();
//            edtsBox = new EdtsBox();
//            edtsBox.parseSub();
//            mdiaBox = new MdiaBox();
//            mdiaBox.parseSub();
        }
    }

    public class TkhdBox extends Box {
        public final static String BOX_NAME = "tkhd";
        private TrakBox trakBox = null;
        public String flags = null;
        public String creationTime = null;
        public String modificationTime = null;
        public String trackId = null;
        public String duration = null;
        public String layer = null;
        public String altemateGroup = null;
        public int volume = 0;
        public int width = 0;
        public int height = 0;

        public EdtsBox edts = null;
        public MdiaBox mdia = null;

        public TkhdBox(TrakBox trakBox, long length) throws IOException {
            super(true, length);
            this.trakBox = trakBox;
        }

        @Override
        protected void parseSub() throws Exception {
            trakBox.isMovie = width > 0 && height > 0;
        }
    }

    public class EdtsBox extends Box {
        public final static String BOX_NAME = "edts";
        public ElstBox elstBox = null;

        public EdtsBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class ElstBox extends Box {
        public final static String BOX_NAME = "elst";
        public String segmentDuration = null;
        public String mediaTime = null;
        public String mediaRate = null;

        public ElstBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class MdiaBox extends Box {
        public final static String BOX_NAME = "mdia";
        public MdhdBox mdhdBox = null;
        public HdlrBox hdlrBox = null;
        public MinfBox minfBox = null;

        public MdiaBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
//            mdhdBox = new MdhdBox();
//            mdhdBox.parseSub();
//            hdlrBox = new HdlrBox();
//            hdlrBox.parseSub();
//            minfBox = new MinfBox();
//            minfBox.parseSub();
        }

    }

    public class MdhdBox extends Box {
        public final static String BOX_NAME = "mdhd";
        public String creationTime = null;
        public String modificationTime = null;
        public String timeScale = null;
        public String duration = null;
        public String language = null;

        public MdhdBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class HdlrBox extends Box {
        public final static String BOX_NAME = "hdlr";
        public String handlerType = null;
        public String name = null;

        public HdlrBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class MinfBox extends Box {
        public final static String BOX_NAME = "minf";
        public VmhdBox vmhdBox = null;
        public DinfBox dinfBox = null;
        public StblBox stblBox = null;

        public MinfBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
//            vmhdBox = new VmhdBox();
//            vmhdBox.parseSub();
//            dinfBox = new DinfBox();
//            dinfBox.parseSub();
//            stblBox = new StblBox();
//            stblBox.parseSub();
        }
    }

    public class VmhdBox extends Box {
        public final static String BOX_NAME = "vmhd";

        public VmhdBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class DinfBox extends Box {
        public final static String BOX_NAME = "dinf";
        public DrefBox drefBox = null;

        public DinfBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
//            drefBox = new DrefBox();
//            drefBox.parseSub();
        }
    }

    public class DrefBox extends Box {
        public final static String BOX_NAME = "dref";
        public String location = null;

        public DrefBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    public class StblBox extends Box {
        public final static String BOX_NAME = "stbl";
        public StsdBox stsdBox = null;
        public SttsBox sttsBox = null;
        public CttsBox cttsBox = null;
        public StssBox stssBox = null;
        public StscBox stscBox = null;
        public StszBox stszBox = null;
        public StcoBox stcoBox = null;

        public StblBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        protected void parseSub() throws Exception {
//            stsdBox = new StsdBox();
//            stsdBox.parseSub();
//            sttsBox = new SttsBox();
//            sttsBox.parseSub();
//            cttsBox = new CttsBox();
//            cttsBox.parseSub();
//            stssBox = new StssBox();
//            stssBox.parseSub();
//            stscBox = new StscBox();
//            stscBox.parseSub();
//            stszBox = new StszBox();
//            stszBox.parseSub();
//            stcoBox = new StcoBox();
//            stcoBox.parseSub();
        }
    }

    public class StsdBox extends Box {
        public final static String BOX_NAME = "stsd";
//        public Avc1Box avc1Box = null;
//        private AvcCBox avcCBox = null;

        private byte[] avcC = new byte[]{0x61, 0x76, 0x63, 0x43};

        public byte[] sps = null;
        public byte[] pps = null;

        public StsdBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            for (int index = 0; index < srcData.length; index++) {
                if (srcData[index] != avcC[0]) {
                    continue;
                }
                if (srcData[index + 1] == avcC[1] && srcData[index + 2] == avcC[2] && srcData[index + 3] == avcC[3]) {
                    // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                    int spsStartPos = index + 10;
                    int spsLength = TypeConversion.byteToInt(srcData, spsStartPos, spsStartPos + 1);
                    sps = new byte[spsLength];
                    spsStartPos += 2;
                    System.arraycopy(srcData, spsStartPos, sps, 0, spsLength);

                    int ppsStartPos = spsStartPos + spsLength + 1;
                    int ppsLength = TypeConversion.byteToInt(srcData, ppsStartPos, ppsStartPos + 1);
                    pps = new byte[ppsLength];
                    ppsStartPos += 2;
                    System.arraycopy(srcData, ppsStartPos, pps, 0, ppsLength);
                    break;
                }
            }
        }
    }

//    public class Avc1Box extends Box {
//        public final static String BOX_NAME = "avc1";
//        public AvcCBox avcCBox = null;
//
//        public Avc1Box(long length) throws IOException {
//            super(false, length);
////            srcData = data;
//        }
//
//        @Override
//        protected void parseSub() throws Exception {
////            avcCBox = new AvcCBox();
////            avcCBox.parseSub();
//        }
//    }

    /**
     * “stts”存储了sample的duration，描述了sample时序的映射方法，我们通过它可以找到任何时间的sample。
     * “stts”可以包含一个压缩的表来映射时间和sample序号，用其他的表来提供每个sample的长度和指针。
     * 表中每个条目提供了在同一个时间偏移量里面连续的sample序号，以及samples的偏移量。
     * 递增这些偏移量，就可以建立一个完整的time to sample表。
     */
    public class SttsBox extends Box {
        public final static String BOX_NAME = "stts";
        public int[] sttsArray;

        public SttsBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            sttsArray = parseArray();
        }
    }

    public class CttsBox extends Box {
        public final static String BOX_NAME = "ctts";

        public CttsBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
        }
    }

    /**
     * “stss”确定media中的关键帧。对于压缩媒体数据，关键帧是一系列压缩序列的开始帧，其解压缩时不依赖以前的帧，而后续帧的解压缩将依赖于这个关键帧。
     * “stss”可以非常紧凑的标记媒体内的随机存取点，它包含一个sample序号表，表内的每一项严格按照sample的序号排列，说明了媒体中的哪一个sample是关键帧。
     * 如果此表不存在，说明每一个sample都是一个关键帧，是一个随机存取点。
     */
    public class StssBox extends Box {
        public final static String BOX_NAME = "stss";
        public int[] stssArray;

        public StssBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            stssArray = parseArray();
        }
    }

    /**
     * 用chunk组织sample可以方便优化数据获取，一个thunk包含一个或多个sample。
     * “stsc”中用一个表描述了sample与chunk的映射关系，查看这张表就可以找到包含指定sample的thunk，从而找到这个sample。
     */
    public class StscBox extends Box {
        public final static String BOX_NAME = "stsc";
        public List<Stsc> stscList;
        public long count = 0;

        public StscBox(long length) throws IOException {
            super(true, length);
        }

        public class Stsc {
            public int firstChunk = 0;
            public int SamplesPerChunk = 0;
            public int SampleDescriptionIndex = 0;
        }

        @Override
        protected void parseSub() throws Exception {
            count = TypeConversion.byteToLong(srcData);
            stscList = new ArrayList<>((int) count);
            for (int index = 8; index < srcData.length; index += 4) {
                Stsc stsc = new Stsc();
                stsc.firstChunk = TypeConversion.byteToInt(srcData, index);
                index += 4;
                stsc.SamplesPerChunk = TypeConversion.byteToInt(srcData, index);
                index += 4;
                stsc.SampleDescriptionIndex = TypeConversion.byteToInt(srcData, index);
                stscList.add(stsc);
            }
        }
    }

    /**
     * “stsz” 定义了每个sample的大小，包含了媒体中全部sample的数目和一张给出每个sample大小的表。这个box相对来说体积是比较大的。
     */
    public class StszBox extends Box {
        public final static String BOX_NAME = "stsz";
        public int[] stszArray;

        public StszBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            stszArray = parseArray();
        }
    }

    /**
     * “stco”定义了每个thunk在媒体流中的位置。
     * 位置有两种可能，32位的和64位的，后者对非常大的电影很有用。在一个表中只会有一种可能，这个位置是在整个文件中的，而不是在任何box中的，
     * 这样做就可以直接在文件中找到媒体数据，而不用解释box。需要注意的是一旦前面的box有了任何改变，这张表都要重新建立，因为位置信息已经改变了。
     */
    public class StcoBox extends Box {
        public final static String BOX_NAME = "stco";
        public int[] stcoArray;

        public StcoBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        protected void parseSub() throws Exception {
            stcoArray = parseArray();
        }
    }

}
