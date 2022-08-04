import java.io.PrintWriter;

public class ArcadeReader {
    public ArcadeReader() {
        /*
            Hiragana: 3041-3096
            Katakana: 30A0-30FF
            Kanji: 3400-4DB5,4E00-9FCB,F900-FA6A
            Kanji Radicals: 2E80-2FD5
        */
        PrintWriter pw = new PrintWriter(System.out,true);
        pw.printf("0x3041~0x3096:\n");
        for (char i=0x3041;i<=0x3096;i++) {
            pw.print(i);
        }
        pw.printf("\n");
        pw.printf("0x30A0~0x30FF:\n");
        for (char i=0x30A0;i<=0x30FF;i++) {
            pw.print(i);
        }
        pw.printf("\n");
        pw.printf("0x3400~0x4DB5:\n");
        for (char i=0x3400;i<=0x4DB5;i++) {
            pw.print(i);
        }
        pw.printf("\n");
        pw.printf("0x4E00~0x9FCB:\n");
        for (char i=0x4E00;i<=0x9FCB;i++) {
            pw.print(i);
        }
        pw.printf("\n");
        pw.printf("0xF900~0xFA6A:\n");
        for (char i=0xF900;i<=0xFA6A;i++) {
            pw.print(i);
        }
        pw.printf("\n");
        pw.printf("0x2E80~0x2FD5:\n");
        for (char i=0x2E80;i<=0x2FD5;i++) {
            pw.print(i);
        }
        pw.printf("\n");
    }
}
