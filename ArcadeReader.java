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
        char totalCharacters = (0x3097-0x3041)+(0x3100-0x30A0)+(0x4DB6-0x3400)+(0x9FCC-0x4E00)+(0xFA6B-0xF900)+(0x2FD6-0x2E80)+(0xFF5F-0xFF01);
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
            char[] ranges=new char[]{
                0x3041,0x3096,
                0x30A0,0x30FF,
                0x3400,0x4DB5,
                0x4E00,0x9FCB,
                0xF900,0xFA6A,
                0xFF01,0xFF5E,
                0x2E80,0x2FD5,
            };
            char[] character = new char[totalCharacters];
            int[][] pixelData = new int[totalCharacters][128*128];
            for (int i=0;i<ranges.length;i+=2) {
                for (char j=ranges[i];j<=ranges[i+1];j++) {
                    g.clearRect(0, 0, 128, 128);
                    g.drawString(Character.toString(j),32,96);
                    //Detect highest and leftest pixels.
                    int topMost=128;
                    int leftMost=128;
                    for (int x=0;x<img.getWidth();x++) {
                        for (int y=0;y<img.getHeight();y++) {
                            if (img.getRGB(x, y)==Color.WHITE.getRGB()) {
                                if (x<leftMost) {
                                    leftMost=x;
                                }
                                if (y<topMost) {
                                    topMost=y;
                                }
                            }
                        }
                    }
                    g.clearRect(0, 0, 128, 128);
                    g.drawString(Character.toString(j),32-leftMost,96-topMost);
                    for (int x=0;x<img.getWidth();x++) {
                        for (int y=0;y<img.getHeight();y++) {
                            character[counter]=j;
                            pixelData[counter][y*128+x]=img.getRGB(x,y);
                        }
                    }
                    counter++;
                }
            }
            g.dispose();
            //ImageIO.write(img,"png",new File("character.png"));
            Image input = ImageIO.read(new File("character.png"));
            char closest = getClosestCharacter(character,pixelData,input);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
        
    }
}
