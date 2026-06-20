package source.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class Util {
    public static ImageIcon createPieceIcon(String path, boolean isRotated) {
        try {
            BufferedImage original = ImageIO.read(new File(path));
            if (!isRotated) return new ImageIcon(original);
            
            // 180度回転処理
            BufferedImage rotated = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
            Graphics2D g2d = rotated.createGraphics();
            AffineTransform at = new AffineTransform();
            at.rotate(Math.PI, original.getWidth() / 2.0, original.getHeight() / 2.0);
            g2d.setTransform(at);
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            return new ImageIcon(rotated);
        } catch (Exception e) { return null; }
    }
}
