package server.avdecode.mp4;

public class NalAnalysis {

    public static boolean checkTag(byte[] nal, int index) {
        if (nal[index] == 0) {
            if (nal[index + 1] == 0 && nal[index + 2] == 0 && nal[index + 3] == 1) {
                return true;
            }
        }
        return false;
    }

    public static byte[] findSPSAndPPS(byte[] nal, int sIndex) {
        int eIndex = sIndex;
        for (int index = sIndex; index < nal.length; index++) {
            if (checkTag(nal, index)) {
                //find start tag
                byte[] data = new byte[eIndex - sIndex];
                System.arraycopy(nal, sIndex, data, 0, data.length);
                return data;
            }
            eIndex++;
        }
        return null;
    }

    public static byte[] splitData(byte[] nal, int sIndex, int eIndex) {
        if (sIndex == eIndex || eIndex < sIndex) {
            return null;
        }
        byte[] data = new byte[eIndex - sIndex];
        System.arraycopy(nal, sIndex, data, 0, data.length);
        return data;
    }

    private static void saveResult(NalInfo nalInfo, int type, byte[] data) {
        switch (type) {
            case 7:
                //sps
                nalInfo.setSPS(data);
                nalInfo.setHasPPSAndSPS(true);
                break;
            case 8:
                //pps
                nalInfo.setPPS(data);
                nalInfo.setHasPPSAndSPS(true);
                break;
            case 5:
            case 0:
            case 1:
            default:
                nalInfo.setType(type);
                nalInfo.setIDR(data);
                break;
        }
    }

    public static NalInfo analysis(byte[] nal) {
        int index = 0;
        NalInfo nalInfo = new NalInfo();
        int type;
        do {
            if (checkTag(nal, index)) {
                index += 4;
                type = nal[index] & 0x1F;
                index++;

                if (type == 7 || type == 8) {
                    byte[] data = findSPSAndPPS(nal, index);
                    saveResult(nalInfo, type, data);
                    index += data.length;
                } else {
                    byte[] data = splitData(nal, index, nal.length);
                    saveResult(nalInfo, type, data);
                }
            } else {
                return null;
            }
        } while (type == 7 || type == 8);

        return nalInfo;
    }


    public static class NalInfo {

        private boolean isHasPPSAndSPS = false;

        private int type = 0;

        private byte[] pps = null;

        private byte[] sps = null;

        //非IDR图像的片  IDR图像中的片 type == 1 type == 5
        private byte[] idr = null;

        public boolean isHasPPSAndSPS() {
            return isHasPPSAndSPS;
        }

        public void setHasPPSAndSPS(boolean hasPPSAndSPS) {
            isHasPPSAndSPS = hasPPSAndSPS;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public byte[] getPPS() {
            return pps;
        }

        public void setPPS(byte[] pps) {
            this.pps = pps;
        }

        public byte[] getSPS() {
            return sps;
        }

        public void setSPS(byte[] sps) {
            this.sps = sps;
        }

        public byte[] getIDR() {
            return idr;
        }

        public void setIDR(byte[] idr) {
            this.idr = idr;
        }
    }
}
