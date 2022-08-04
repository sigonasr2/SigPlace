import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Color;

public class ArcadeReader {
    public ArcadeReader() {
        /*
            Noto Sans Japanese
            Hiragana: 3041-3096
            Katakana: 30A0-30FF
            Kanji: 3400-4DB5,4E00-9FCB,F900-FA6A
            Kanji Radicals: 2E80-2FD5
            Alphanumeric/Punctuation: FF01-FF5E
        */
        char totalCharacters = (0x3097-0x3041)+(0x3100-0x30A0)+(0x4DB6-0x3400)+(0x9FCC-0x4E00)+(0xFA6B-0xF900)+(0x2FD6-0x2E80)-(0xFF5F-0xFF01);
        System.out.println("Total Characters: "+(int)totalCharacters);
        /*PrintWriter pw = new PrintWriter(System.out,true);
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
        pw.printf("\n");*/
        try {
            Font font;
            font = Font.createFont(Font.TRUETYPE_FONT, new File("NotoSansJP-Bold.otf"));
            font  = font.deriveFont(Font.BOLD, 64);
    
            BufferedImage img = new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setFont(font);
            g.setColor(Color.WHITE);
            g.setBackground(Color.BLACK);
            int counter=0;
            for (char i=0x3041;i<=0x3096;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0x30A0;i<=0x30FF;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0x3400;i<=0x4DB5;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0x4E00;i<=0x9FCB;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0xF900;i<=0xFA6A;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0x2E80;i<=0x2FD5;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            for (char i=0xFF01;i<=0xFF5E;i++) {
                g.clearRect(0, 0, 128, 128);
                g.drawString(Character.toString(i),32,96);
            }
            ImageIO.write(img,"png",new File("character.png"));
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
        
    }
}
