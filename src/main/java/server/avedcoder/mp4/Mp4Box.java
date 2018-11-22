package server.avedcoder.mp4;




import util.LogDog;

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
    private byte[] buffer = new byte[8];

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

    private int byteToInt(byte[] src, int offset) {
        int value;
        value = (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

    private long byteToLong(byte[] byteNum) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    private long byteToLong(byte[] byteNum, int start, int end) {
        long num = start;
        for (int ix = 0; ix < end - start; ++ix) {
            num <<= end - start;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }

    private String byteToHexStr(byte[] b) {
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < b.length; n++) {
            String tmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((tmp.length() == 1) ? "0" + tmp : tmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    /**
     * 分析MP4文件的所有box信息
     *
     * @throws IOException
     */
    public void analysis() throws Exception {
        mp4File.seek(0);
        while (mp4File.getFilePointer() < mp4File.length()) {
            int ret = mp4File.read(buffer);
            if (ret < 1) {
                throw new IOException("file is end or stream has error  !");
            }
            if (validBoxName(buffer)) {
                if (buffer[3] == 1) {
                    // 64 bits atom size
                } else {
                    // 32 bits atom size
                    long length = byteToInt(buffer, 0);
                    String name = new String(buffer, 4, 4);
                    saveBox(name, length);
                }
            }
        }
    }

    public String transForLongToDate(Long millSec) {
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
            case Avc1Box.BOX_NAME:
                if (currentTrakBox != null) {
                    Avc1Box avc1Box = new Avc1Box(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stsdBox.avc1Box = avc1Box;
                    avc1Box.parseSub();
                }
                break;
            case AvcCBox.BOX_NAME:
                if (currentTrakBox != null) {
                    AvcCBox avcCBox = new AvcCBox(length);
                    currentTrakBox.mdiaBox.minfBox.stblBox.stsdBox.avc1Box.avcCBox = avcCBox;
                    avcCBox.parseSub();
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
                skipBox(length);
                break;
        }
    }

    private void skipBox(long length) throws IOException {
        //当前的box不对应,跳过这个box
        if (length > Integer.MAX_VALUE) {
            while (length > 0) {
                int tmp = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (length - buffer.length);
                mp4File.skipBytes(tmp);
                length -= tmp;
            }
        } else {
            mp4File.skipBytes((int) length - buffer.length);
        }
    }


    public FtypBox getFtypBox() {
        return ftypBox;
    }

    public void setFtypBox(FtypBox ftypBox) {
        this.ftypBox = ftypBox;
    }

    public MoovBox getMoovBox() {
        return moovBox;
    }

    public void setMoovBox(MoovBox moovBox) {
        this.moovBox = moovBox;
    }

    public void release() {
        moovBox.release();
        ftypBox.release();
        buffer = null;
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
        public byte[] srcData = null;

        public Box(boolean isHasSrcData, long length) throws IOException {
            if (isHasSrcData) {
                srcData = new byte[(int) length - buffer.length];
                mp4File.read(srcData);
                LogDog.i("==> srcData " + getClass().getName() + " = " + byteToHexStr(srcData));
            }
        }

        abstract void parseSub() throws Exception;

        public void release() {
            srcData = null;
        }
    }

    /*-------------------------------下面是所有Box------------------------------*/

    private class FtypBox extends Box {
        public final static String BOX_NAME = "ftyp";
        public String majorBrand = null;
        public String minorVersion = null;
        public String compatibleBrands = null;

        public FtypBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
            int index = 0;
            majorBrand = new String(srcData, index, index + 4);
            index += 4;
            minorVersion = String.valueOf(byteToInt(srcData, index));
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
            long time = byteToLong(srcData);
            creationTime = transForLongToDate(time);
            long mtime = byteToLong(srcData, 8, 12);
            modificationTime = transForLongToDate(mtime);
            timeScale = byteToInt(srcData, 12);
            duration = byteToInt(srcData, 16);
            nextTrackId = byteToInt(srcData, srcData.length - 4);
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
        }
    }

    public class DinfBox extends Box {
        public final static String BOX_NAME = "dinf";
        public DrefBox drefBox = null;

        public DinfBox(long length) throws IOException {
            super(false, length);
        }

        @Override
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        void parseSub() throws Exception {
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
        public Avc1Box avc1Box = null;

        public StsdBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
//            avc1Box = new Avc1Box();
//            avc1Box.parseSub();
        }
    }

    public class Avc1Box extends Box {
        public final static String BOX_NAME = "avc1";
        public AvcCBox avcCBox = null;

        public Avc1Box(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
//            avcCBox = new AvcCBox();
//            avcCBox.parseSub();
        }
    }

    public class AvcCBox extends Box {
        public final static String BOX_NAME = "avcC";

        public AvcCBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class SttsBox extends Box {
        public final static String BOX_NAME = "stts";

        public SttsBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class CttsBox extends Box {
        public final static String BOX_NAME = "ctts";

        public CttsBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class StssBox extends Box {
        public final static String BOX_NAME = "stss";

        public StssBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class StscBox extends Box {
        public final static String BOX_NAME = "stsc";

        public StscBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class StszBox extends Box {
        public final static String BOX_NAME = "stsz";

        public StszBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

    public class StcoBox extends Box {
        public final static String BOX_NAME = "stco";

        public StcoBox(long length) throws IOException {
            super(true, length);
        }

        @Override
        void parseSub() throws Exception {
        }
    }

}
