/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cardimagedownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author alfieriv
 */
public class DownloaderGUI extends javax.swing.JFrame {

    public static void main(String[] args) {
        DownloaderGUI gui = new DownloaderGUI();
        gui.setLocationRelativeTo(null);
        gui.setVisible(true);
    }
    
    PrintWriter cardlist;
    HashMap<String, HashMap<String, String>> database;
    
    public void loadDatabase(){
        if(database != null)
            database.clear();
        database = new HashMap<String, HashMap<String, String>>();
        try {
            String databaseurl = "https://github.com/WagicProject/wagic/releases/latest/download/CardImageLinks.csv";
            URL url = new URL(databaseurl);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            if(httpcon == null) {
                database = null;
                loaded = false;
                return;
            }
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            InputStream in;
            try{
                in = new BufferedInputStream(httpcon.getInputStream());
            }catch(Exception ex){
                try {
                    in = new BufferedInputStream(httpcon.getInputStream());
                } catch (Exception ex2) {
                    try {
                        in = new BufferedInputStream(httpcon.getInputStream());
                    } catch (Exception ex3) {
                        database = null;
                        loaded = false;
                        return;
                    }
                }
            }
            
            String path = new File(DownloaderGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            if(path.contains(".jar"))
                path = path.substring(0,path.length() - 24);
            
            String databasepath = path + File.separator + "CardImageLinks.csv";
            
            ReadableByteChannel readableByteChannel2 = Channels.newChannel(httpcon.getInputStream());
            FileOutputStream fileOutputStream2 = new FileOutputStream(databasepath);
            FileChannel fileChannel2 = fileOutputStream2.getChannel();
            fileOutputStream2.getChannel().transferFrom(readableByteChannel2, 0, Long.MAX_VALUE);
            fileChannel2.close();
            fileOutputStream2.close();
            readableByteChannel2.close();
            
            String lines = readLineByLineJava8(databasepath);
            String[] rows = lines.split("\n");
            for(int i = 1; i < rows.length; i++){
                String[] cols = rows[i].split(";");
                if(database.get(cols[0]) == null)
                    database.put(cols[0], new HashMap<String, String>());
                database.get(cols[0]).put(cols[1], cols[2]);
            }
            File del = new File(databasepath);
            del.delete();
        } catch (Exception e) {
            database = null;
            loaded = false;
        }
        loaded = true;
    }
    
    public boolean fastDownloadCard(String set, String id, String name, String imgPath, String thumbPath, int ImgX, int ImgY, int ThumbX, int ThumbY, int Border, int BorderThumb){
        if(database == null)
            return false;
        HashMap<String, String> subdb = database.get(set);
        if(subdb == null)
            return false;
        String imageurl = subdb.get(id);
        if(imageurl == null)
            return false;
        try{   
            URL url = new URL(imageurl);
            if(url == null) {
                setTextArea("Warning: Problem fetching card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                return false;
            }
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            if(httpcon == null) {
                setTextArea("Warning: Problem fetching card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                return false;
            }
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            httpcon.setConnectTimeout(5000);
            httpcon.setReadTimeout(5000);
            httpcon.setAllowUserInteraction(false);
            httpcon.setDoInput(true);
            httpcon.setDoOutput(false);
            InputStream in;
            try{
                in = new BufferedInputStream(httpcon.getInputStream());
            }catch(Exception ex){
                setTextArea("Warning: Problem downloading card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                try {
                    in = new BufferedInputStream(httpcon.getInputStream());
                } catch (Exception ex2) {
                    setTextArea("Warning: Problem downloading card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                    try {
                        in = new BufferedInputStream(httpcon.getInputStream());
                    } catch (Exception ex3) {
                        setTextArea("Warning: Problem downloading card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                        return false;
                    }
                }
            }
            
            String cardimage = imgPath + File.separator + id + ".jpg";
            String thumbcardimage = thumbPath + File.separator + id + ".jpg";
            
            ReadableByteChannel readableByteChannel = Channels.newChannel(httpcon.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(cardimage);
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileChannel.close();
            fileOutputStream.close();
                
            fileOutputStream = new FileOutputStream(thumbcardimage);
            fileChannel = fileOutputStream.getChannel();
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileChannel.close();
            fileOutputStream.close();
            readableByteChannel.close();
                
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            MediaTracker tracker = new MediaTracker(new Panel());
            Image image = toolkit.getImage(cardimage);
            tracker.addImage(image, 0);
            try {
                tracker.waitForAll();
            } catch (Exception e) { }
            
            BufferedImage resizedImg = new BufferedImage(ImgX, ImgY, BufferedImage.TYPE_INT_RGB);
            Graphics2D tGraphics2DReiszed = resizedImg.createGraphics(); //create a graphics object to paint to
            if(set.equals("2ED") || set.equals("RV") || set.equals("4ED") || set.equals("5ED") || set.equals("6ED") || set.equals("7ED") || 
                    set.equals("8ED") || set.equals("9ED") || set.equals("CHR") || set.equals("DM") || set.equals("S00") || set.equals("S99") || 
                    set.equals("PTK") || set.equals("BTD") || set.equals("ATH") || set.equals("BRB")){
                tGraphics2DReiszed.setBackground(Color.WHITE);
                tGraphics2DReiszed.setPaint(Color.WHITE);
            }else {
                tGraphics2DReiszed.setBackground(Color.BLACK);
                tGraphics2DReiszed.setPaint(Color.BLACK);
            }
            tGraphics2DReiszed.fillRect(0, 0, ImgX, ImgY);
            tGraphics2DReiszed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            tGraphics2DReiszed.drawImage(image, 0, 0, ImgX, ImgY, null); //draw the image scaled
            resizedImg = resizedImg.getSubimage(Border, Border, ImgX-2*Border, ImgY-2*Border);
            ImageIO.write(resizedImg, "JPG", new File(cardimage)); //write the image to a file

            BufferedImage tThumbImage = new BufferedImage(ThumbX, ThumbY, BufferedImage.TYPE_INT_RGB);
            Graphics2D tGraphics2D = tThumbImage.createGraphics(); //create a graphics object to paint to
            if(set.equals("2ED") || set.equals("RV") || set.equals("4ED") || set.equals("5ED") || set.equals("6ED") || set.equals("7ED") || 
                    set.equals("8ED") || set.equals("9ED") || set.equals("CHR") || set.equals("DM") || set.equals("S00") || set.equals("S99") || 
                    set.equals("PTK") || set.equals("BTD") || set.equals("ATH") || set.equals("BRB")){
                tGraphics2D.setBackground(Color.WHITE);
                tGraphics2D.setPaint(Color.WHITE);
            }else {
                tGraphics2D.setBackground(Color.BLACK);
                tGraphics2D.setPaint(Color.BLACK);
            }
            tGraphics2D.fillRect(0, 0, ThumbX, ThumbY);
            tGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            tGraphics2D.drawImage(image, 0, 0, ThumbX, ThumbY, null); //draw the image scaled
            tThumbImage = tThumbImage.getSubimage(BorderThumb, BorderThumb, ThumbX-2*BorderThumb, ThumbY-2*BorderThumb);
            ImageIO.write(tThumbImage, "JPG", new File(thumbcardimage)); //write the image to a file
            
        }catch (Exception e){
            setTextArea("Warning: Problem fetching card: " + name + " (" + id + ".jpg) from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
            return false;
        }
        imageurl = subdb.get(id + "t");
        if(imageurl != null && !imageurl.isEmpty()){
            setTextArea("The card: " + name + " (" + id + ".jpg) can create a token, i will try to download that image too as " + id + "t.jpg", Color.black, new Font("Arial", 1, 14));
            try{   
                URL url = new URL(imageurl);
                if(url == null){
                    setTextArea("Warning: Problem fetching token: " + id + "t.jpg from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                    return false;
                }
                HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
                if(httpcon == null) {
                    setTextArea("Warning: Problem fetching token: " + id + "t.jpg from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                    return false;
                }
                httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
                httpcon.setConnectTimeout(5000);
                httpcon.setReadTimeout(5000);
                httpcon.setAllowUserInteraction(false);
                httpcon.setDoInput(true);
                httpcon.setDoOutput(false);
                InputStream in;
                try{
                    in = new BufferedInputStream(httpcon.getInputStream());
                }catch(Exception ex){
                    setTextArea("Warning: Problem downloading token: " + id + "t.jpg from " + imageurl + ", i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                    try {
                        in = new BufferedInputStream(httpcon.getInputStream());
                    } catch (Exception ex2) {
                        setTextArea("Warning: Problem downloading token: " + id + "t.jpg from " + imageurl + ", i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                        try {
                            in = new BufferedInputStream(httpcon.getInputStream());
                        } catch (Exception ex3) {
                            setTextArea("Warning: Problem downloading token: " + id + "t.jpg from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                            return false;
                        }
                    }
                }                                
                
                String cardimage = imgPath + File.separator + id + "t.jpg";
                String thumbcardimage = thumbPath + File.separator + id + "t.jpg";
                
                ReadableByteChannel readableByteChannel = Channels.newChannel(httpcon.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(cardimage);
                FileChannel fileChannel = fileOutputStream.getChannel();
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                fileChannel.close();
                fileOutputStream.close();
                
                fileOutputStream = new FileOutputStream(thumbcardimage);
                fileChannel = fileOutputStream.getChannel();
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                fileChannel.close();
                fileOutputStream.close();
                readableByteChannel.close();
                
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                MediaTracker tracker = new MediaTracker(new Panel());
                Image image = toolkit.getImage(cardimage);
                tracker.addImage(image, 0);
                try {
                    tracker.waitForAll();
                } catch (Exception e) { }
                
                BufferedImage resizedImg = new BufferedImage(ImgX, ImgY, BufferedImage.TYPE_INT_RGB);
                Graphics2D tGraphics2DReiszed = resizedImg.createGraphics(); //create a graphics object to paint to
                if(set.equals("2ED") || set.equals("RV") || set.equals("4ED") || set.equals("5ED") || set.equals("6ED") || set.equals("7ED") || 
                        set.equals("8ED") || set.equals("9ED") || set.equals("CHR") || set.equals("DM") || set.equals("S00") || set.equals("S99") || 
                        set.equals("PTK") || set.equals("BTD") || set.equals("ATH") || set.equals("BRB")){
                    tGraphics2DReiszed.setBackground(Color.WHITE);
                    tGraphics2DReiszed.setPaint(Color.WHITE);
                }else {
                    tGraphics2DReiszed.setBackground(Color.BLACK);
                    tGraphics2DReiszed.setPaint(Color.BLACK);
                }
                tGraphics2DReiszed.fillRect(0, 0, ImgX, ImgY);
                tGraphics2DReiszed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                tGraphics2DReiszed.drawImage(image, 0, 0, ImgX, ImgY, null); //draw the image scaled
                resizedImg = resizedImg.getSubimage(Border, Border, ImgX-2*Border, ImgY-2*Border);
                ImageIO.write(resizedImg, "JPG", new File(cardimage)); //write the image to a file

                BufferedImage tThumbImage = new BufferedImage(ThumbX, ThumbY, BufferedImage.TYPE_INT_RGB);
                Graphics2D tGraphics2D = tThumbImage.createGraphics(); //create a graphics object to paint to
                if(set.equals("2ED") || set.equals("RV") || set.equals("4ED") || set.equals("5ED") || set.equals("6ED") || set.equals("7ED") || 
                        set.equals("8ED") || set.equals("9ED") || set.equals("CHR") || set.equals("DM") || set.equals("S00") || set.equals("S99") || 
                        set.equals("PTK") || set.equals("BTD") || set.equals("ATH") || set.equals("BRB")){
                    tGraphics2D.setBackground(Color.WHITE);
                    tGraphics2D.setPaint(Color.WHITE);
                }else {
                    tGraphics2D.setBackground(Color.BLACK);
                    tGraphics2D.setPaint(Color.BLACK);
                }
                tGraphics2D.fillRect(0, 0, ThumbX, ThumbY);
                tGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                tGraphics2D.drawImage(image, 0, 0, ThumbX, ThumbY, null); //draw the image scaled
                tThumbImage = tThumbImage.getSubimage(BorderThumb, BorderThumb, ThumbX-2*BorderThumb, ThumbY-2*BorderThumb);
                ImageIO.write(tThumbImage, "JPG", new File(thumbcardimage)); //write the image to a file
            }catch (Exception e){
                setTextArea("Warning: Problem fetching token: " + id + "t.jpg from " + imageurl + ", i will try with slow method...", Color.blue, new Font("Arial", 1, 14));
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creates new form DownloaderGUI
     */
    public DownloaderGUI() {
        initComponents();
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new CheckableItem[] { new CheckableItem("*.* - All Wagic sets (thousands of cards)", true) }));
        jTextPane1.setAutoscrolls(true);
        jTextPane1.setEditable(false);
        DefaultCaret caret = (DefaultCaret)jTextPane1.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        try {
            String path = new File(DownloaderGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            if(path.contains(".jar"))
                path = path.substring(0,path.length() - 24);
            //cardlist = new PrintWriter(path + File.separator + "CardImageList_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv");
        } catch (Exception ex) {
            cardlist = null;
        }
        if(cardlist != null)
            cardlist.append("id;set;link\n");
    }

    public JProgressBar getProgressBar(){
        return jProgressBar1;
    }
    
    public JComboBox getSetCombo(){
        return jComboBox1;
    }
    
    private String readLineByLineJava8(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.ISO_8859_1))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (Exception e)
        {
            setTextArea("Error parsing content of file: " + filePath, Color.red, new Font("Arial", 1, 14));
        }

        return contentBuilder.toString();
    }

    public JTextPane getTextArea() {
        return jTextPane1;
    }
    
    public void setTextArea(String text, Color color, Font font) {
        SimpleAttributeSet keyWord = new SimpleAttributeSet();
        StyleConstants.setForeground(keyWord, color);
        StyleConstants.setBold(keyWord, font.isBold());
        StyleConstants.setFontSize(keyWord, font.getSize());
        if(!jTextPane1.getText().isEmpty()){
            try {
                javax.swing.text.Document doc = jTextPane1.getDocument();
                doc.insertString(doc.getLength(), "\r\n" + text, keyWord);
                if(logger != null)
                    logger.append("\n" + text);
            } catch(Exception exc) {}
        } else {
            try {
                javax.swing.text.Document doc = jTextPane1.getDocument();
                doc.insertString(0, text, keyWord);
                if(logger != null)
                    logger.append(text);
            } catch(Exception exc) {}
        }
        jTextPane1.setCaretPosition(jTextPane1.getDocument().getLength());
        if(logger != null)
            logger.flush();
    }
    
    public String getSetInfo(String setName, boolean zipped, String path){
        String cardsfilepath = "";
        boolean todelete = false;
        if(zipped){
            File resFolder = new File(path + File.separator + "Res" + File.separator);
            File [] listOfFile = resFolder.listFiles();
            ZipFile zipFile = null;
            InputStream stream;
            java.nio.file.Path filePath;
            try {
                zipFile = new ZipFile(path + File.separator + "Res" + File.separator + listOfFile[0].getName());
                Enumeration<? extends ZipEntry> e = zipFile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String entryName = entry.getName();
                    if(entryName != null && entryName.contains("sets/")){
                        if(entryName.contains("_cards.dat")){
                            String[] names = entryName.split("/");
                            if(setName.equalsIgnoreCase(names[1])){
                                stream = zipFile.getInputStream(entry);
                                byte[] buffer = new byte[1];
                                java.nio.file.Path outDir = Paths.get(path + File.separator + "Res" + File.separator);
                                filePath = outDir.resolve("_cards.dat");
                                try {
                                    FileOutputStream fos = new FileOutputStream(filePath.toFile());
                                    BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                                    int len;
                                    while ((len = stream.read(buffer)) != -1) {
                                        bos.write(buffer, 0, len);
                                    }
                                    fos.close();
                                    bos.close();
                                    cardsfilepath = filePath.toString();
                                    todelete = true;
                                } catch (Exception ex) {
                                    setTextArea("Error extracting Res zip file: " + ex, Color.red, new Font("Arial", 1, 14));
                                }
                                break;
                            }		
                        }
                    }
                }	    
            } catch (Exception ioe){ } 
            finally {
                try {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                } catch (Exception ioe) {}
            }
        } else {
            File setFolder = new File(path + File.separator + "Res" + File.separator + "sets" + File.separator + setName + File.separator);
            cardsfilepath = setFolder.getAbsolutePath() + File.separator + "_cards.dat";
        }
        String lines = readLineByLineJava8(cardsfilepath);
        if(todelete) {
            File del = new File(cardsfilepath);
            del.delete();
        }
        int totalcards;
        String findStr = "total=";
        int lastIndex = lines.indexOf(findStr);
        String totals = lines.substring(lastIndex, lines.indexOf("\n", lastIndex));
        totalcards = Integer.parseInt(totals.split("=")[1]);
        findStr = "name=";
        lastIndex = lines.indexOf(findStr);
        String name = lines.substring(lastIndex, lines.indexOf("\n", lastIndex)).split("=")[1];
        return name + " (" + totalcards + " cards)";
    }
    
    ArrayList<String> SelectedSets;
    String targetres;
    String WagicPath;
    boolean zipped;
    boolean borderless;
    PrintWriter logger;
    Thread downloader;
    volatile boolean paused;
    volatile boolean interrupted;
    
    public String getSpecialCardUrl(String id, String set){
        String cardurl = "";

        if(id.equals("15208711t"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/c/9c138bf9-8be6-4f1a-a82c-a84938ab84f5.jpg?1562279137";
        else if(id.equals("15208712t"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/4/d453ee89-6122-4d51-989c-e78b046a9de3.jpg?1561758141";
        else if(id.equals("2050321t"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/8/18b9c83d-4422-4b95-9fc2-070ed6b5bdf6.jpg?1562701921";
        else if(id.equals("22010012t"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/4/84dc847c-7a37-4c7f-b02c-30b3e4c91fb6.jpg?1561757490";
        else if(id.equals("4143881t"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/a/8a73e348-5bf1-4465-978b-3f31408bade9.jpg?1561757530";
        else if(id.equals("8759611"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/1/41004bdf-8e09-4b2c-9e9c-26c25eac9854.jpg?1562493483";
        else if(id.equals("8759911"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/b/0b61d772-2d8b-4acf-9dd2-b2e8b03538c8.jpg?1562492461";
        else if(id.equals("8759511"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/2/d224c50f-8146-4c91-9401-04e5bd306d02.jpg?1562496100";
        else if(id.equals("8471611"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/4/84920a21-ee2a-41ac-a369-347633d10371.jpg?1562494702";
        else if(id.equals("8760011"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/2/42ba0e13-d20f-47f9-9c86-2b0b13c39ada.jpg?1562493487";
        else if(id.equals("7448911"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/a/ca03131a-9bd4-4fba-b95c-90f1831e86e7.jpg?1562879774";
        else if(id.equals("7453611"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/3/73636ca0-2309-4bb3-9300-8bd0c0bb5b31.jpg?1562877808";
        else if(id.equals("7447611"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/8/28f72260-c8f9-4c44-92b5-23cef6690fdd.jpg?1562876119";
        else if(id.equals("7467111"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/f/1fe2b76f-ddb7-49d5-933b-ccb06be5d46f.jpg?1562875903";
        else if(id.equals("7409311"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/5/758abd53-6ad2-406e-8615-8e48678405b4.jpg?1562877848";
        else if(id.equals("3896122t"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/9/59a00cac-53ae-46ad-8468-e6d1db40b266.jpg?1562542382";
        else if(id.equals("11492113t"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/b/5b9f471a-1822-4981-95a9-8923d83ddcbf.jpg?1562702075";
        else if(id.equals("3896523t")) //Kraken 9/9
            cardurl = "https://img.scryfall.com/cards/large/front/d/0/d0cd85cc-ad22-446b-8378-5eb69fee1959.jpg?1562840712";
        else if(id.equals("7897511"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/4/a4f4aa3b-c64a-4430-b1a2-a7fca87d0a22.jpg?1562763433";
        else if(id.equals("7868811"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/3/b3523b8e-065f-427c-8d5b-eb731ca91ede.jpg?1562763691";
        else if(id.equals("7868711"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/8/58164521-aeec-43fc-9db9-d595432dea6f.jpg?1564694999";
        else if(id.equals("7868611"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/3/33a8e5b9-6bfb-4ff2-a16d-3168a5412807.jpg?1562758927";
        else if(id.equals("7869111"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/d/9de1eebf-5725-438c-bcf0-f3a4d8a89fb0.jpg?1562762993";
        else if(id.equals("7860011"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/6/864ad989-19a6-4930-8efc-bbc077a18c32.jpg?1562762069";
        else if(id.equals("7867911"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/8/c8265c39-d287-4c5a-baba-f2f09dd80a1c.jpg?1562764226";
        else if(id.equals("7867811"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/0/a00a7180-49bd-4ead-852a-67b6b5e4b933.jpg?1564694995";
        else if(id.equals("7869511"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/2/f2ddf1a3-e6fa-4dd0-b80d-1a585b51b934.jpg?1562765664";
        else if(id.equals("7869411"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/e/6ee6cd34-c117-4d7e-97d1-8f8464bfaac8.jpg?1562761096";
        else if(id.equals("209163t"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/3/a3ea39a8-48d1-4a58-8662-88841eabec92.jpg?1562925559";
        else if(id.equals("111066t"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/7/a77c1ac0-5548-42b0-aa46-d532b3518632.jpg?1562578875";
        else if(id.equals("2050322t") || id.equals("16710t")) //Ooze 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/b/ab430ac0-6fbc-4361-8adc-0c13b399310f.jpg?1562702254";
        else if(id.equals("401721t")) //Hellion 4/4
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/a/da59fb40-b218-452f-b161-3bde15e30c74.jpg?1593142801";
        else if(id.equals("401722t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/3/d365bf32-74a3-436b-b1cc-1a7d3254257a.jpg?1593142810";
        else if(id.equals("19784311t") || id.equals("29669411t") || id.equals("53939511t")) //Snake 1/1 green
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/a/2a452235-cebd-4e8f-b217-9b55fc1c3830.jpg?1562701977";
        else if(id.equals("19784313t") || id.equals("29669413t") || id.equals("53939513t")) //Elephant 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/d/2dbccfc7-427b-41e6-b770-92d73994bf3b.jpg?1562701986";
        else if(id.equals("20787512t")) //Wurm T2 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/6/a6ee0db9-ac89-4ab6-ac2e-8a7527d9ecbd.jpg?1598312477";
        else if(id.equals("20787511t")) //Wurm T1 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b68e816f-f9ac-435b-ad0b-ceedbe72447a.jpg?1598312203";
        else if(id.equals("11492111t")) //Citizen 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/6/165164e7-5693-4d65-b789-8ed8a222365b.jpg?1547509191";
        else if(id.equals("11492112t")) //Camarid 1/1
            cardurl = "https://www.mtg.onl/static/f5ec1ae8d4ec1a8be1c20ec315956bfa/4d406/PROXY_Camarid_U_1_1.jpg";
        else if(id.equals("11492114t") || id.equals("16932t") || id.equals("293980t") || id.equals("293981t") || id.equals("296660t")) //Goblin 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1626139812";
        else if(id.equals("3896522t")) //Whale 6/6
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/7/87a6f719-3e2f-48ea-829d-77134a2a8432.jpg?1618767707";
        else if(id.equals("3896521t")) //Fish 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/a/3abd270d-55d0-40f8-9864-4a7d7b9310ff.jpg?1625974696";
        else if(id.equals("207998t")) //Minion */*
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/9/a9930d11-4772-4fc2-abbd-9af0a9b23a3e.jpg?1561757789";
        else if (id.equals("19784555t")) //Elemental */*
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/6/8676704a-419e-4a00-a052-bca2ad34ecae.jpg?1601138189";
        else if (id.equals("19784612t") || id.equals("53941712t")) //Centaur 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/8/880d5dc1-ceec-4c5f-93c2-c88b7dbfcac2.jpg?1562539811";
        else if (id.equals("19784613t") || id.equals("52973t") || id.equals("53941713t")) //Rhino 4/4
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/3/1331008a-ae86-4640-b823-a73be766ac16.jpg?1562539801";
        else if (id.equals("19784611t") || id.equals("53941711t")) //Knight 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/b/1bc2969b-2176-4471-b316-9c80443866dd.jpg?1562639700";
        else if (id.equals("4977511t")) //Elemental 2/2
            cardurl = "https://www.mtg.onl/static/acc7da698156ddfb2270f09ac7ae6f81/4d406/PROXY_Elemental_U_2_2.jpg";
        else if (id.equals("4977512t")) //Elemental 3/3
            cardurl = "https://www.mtg.onl/static/6c36d944a78a513c082c86b7f624b3b6/4d406/PROXY_Elemental_R_3_3.jpg";
        else if(id.equals("383257t")) //Land mine
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/0/30093c6e-505e-4902-b535-707e364059b4.jpg?1562639734";
        else if(id.equals("383290t")) //Treefolk Warrior */*
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/5/2569593a-d2f2-414c-9e61-2c34e8a5832d.jpg?1562639718";
        else if(id.equals("378445t")) //Gold
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/c/0ca10abd-8d9d-4c0b-9d33-c3516abdf6b3.jpg?1562857254";
        else if(id.equals("16699t"))//Myr 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/b/dbad9b20-0b13-41b9-a84a-06b691ee6c71.jpg?1562542415";
        else if(id.equals("16708t") || id.equals("17097t") || id.equals("17085t")) //Insect 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/4/0436e71b-c1f9-4ca8-a29c-775da858a0cd.jpg?1572892506";
        else if(id.equals("16717t")) //Germ 0/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/4/4414f9fa-dfda-4714-9f87-cb5e8914b07a.jpg?1562702033";
        else if(id.equals("16718t")) //Spawn 2/2
            cardurl = "http://1.bp.blogspot.com/-0-mLvfUVgNk/VmdZWXWxikI/AAAAAAAAAUM/TVCIiZ_c67g/s1600/Spawn%2BToken.jpg";
        else if(id.equals("16729t") || id.equals("17538t")) //Beast 5/5
            cardurl = "https://www.mtg.onl/static/115b4e620e7ac0442355b28e5dc03673/4d406/PROXY_Beast_G_5_5.jpg";
        else if(id.equals("52993t")) //Assembly-Worker 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/7/e72daa68-0680-431c-a616-b3693fd58813.jpg?1619404806";
        else if(id.equals("52593t") || id.equals("294265t")) //Plant 0/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/a/fa0025fa-c530-4151-bcff-48425a4f1db5.jpg?1562230616";
        else if(id.equals("52492t"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/3/f32ad93f-3fd5-465c-ac6a-6f8fb57c19bd.jpg?1561758422";
        else if(id.equals("52418t") || id.equals("378521t")) //Kraken 9/9
            cardurl= "https://c1.scryfall.com/file/scryfall-cards/large/front/d/0/d0cd85cc-ad22-446b-8378-5eb69fee1959.jpg?1562840712";
        else if(id.equals("52398t")) //Illusion 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/1/a10729a5-061a-4daf-91d6-0f6ce813a992.jpg?1562539791";
        else if(id.equals("52149t") || id.equals("52136t")) //Soldier 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/45907b16-af17-4237-ab38-9d7537fd30e8.jpg?1572892483";
        else if(id.equals("52637t") || id.equals("52945t") || id.equals("296637t")) // Thopter 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/a/5a4649cc-07fb-4ff0-9ac6-846763b799df.jpg?1561757203";
        else if(id.equals("74272"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/5/45af7f55-9a69-43dd-969f-65411711b13e.jpg?1562487939";
        else if(id.equals("242498"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/5/f500cb95-d5ea-4cf2-920a-f1df45a9059b.jpg?1581395269";
        else if(id.equals("253431"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/3/1303e02a-ef69-4817-bca5-02c74774b811.jpg?1581395277";
        else if(id.equals("262659"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/f/6f35e364-81d9-4888-993b-acc7a53d963c.jpg?1581395260";
        else if(id.equals("262698"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/2/a2c044c0-3625-4bdf-9445-b462394cecae.jpg?1581395071";
        else if(id.equals("244734"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/b/cb09041b-4d09-4cae-9e85-b859edae885b.jpg?1581718174";
        else if(id.equals("244712"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/c/7c5a3c09-5656-4975-ba03-2d809903ed18.jpg?1581395121";
        else if(id.equals("227405"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/a/6aef77b3-4b38-4902-9f7a-dc18b5bb9da9.jpg?1581395237";
        else if(id.equals("247122"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/6/b6edac85-78e7-4e90-b538-b67c88bb5c62.jpg?1581395155";
        else if(id.equals("244738"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/8/683af377-c491-4f62-900c-6b83d75c33c9.jpg?1581718186";
        else if(id.equals("253429"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/1/b150d71f-11c9-40d6-a461-4967ef437315.jpg?1581395222";
        else if(id.equals("242509"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/3/932d753d-9584-4ad8-9a5e-a3524184f961.jpg?1581395184";
        else if(id.equals("687706") || id.equals("687751"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/9/692c668d-3061-4bdf-921f-94af32b4878c.jpg?1562919765";
        else if(id.equals("414422"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/f/7f95145a-41a1-478e-bf8a-ea8838d6f9b1.jpg?1576384557";
        else if(id.equals("414325"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/a/9a55b60a-5d90-4f73-984e-53fdcc0366e4.jpg?1576383921";
        else if(id.equals("414347"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/2/22e816af-df55-4a3f-a6e7-0ff3bb1b45b5.jpg?1576384071";
        else if(id.equals("414392"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/0/70b94f21-4f01-46f8-ad50-e2bb0b68ea33.jpg?1625771745";
        else if(id.equals("414305"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/a/5a7a212e-e0b6-4f12-a95c-173cae023f93.jpg?1625771723";
        else if(id.equals("414500"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/7/078b2103-15ce-456d-b092-352fa7222935.jpg?1576385054";
        else if(id.equals("414471"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/6/a63c30c0-369a-4a75-b352-edab4d263d1b.jpg?1576384864";
        else if(id.equals("414480"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/d/0dbaef61-fa39-4ea7-bc21-445401c373e7.jpg?1576449562";
        else if(id.equals("414449"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/6/460f7733-c0a6-4439-a313-7b26ae6ee15b.jpg?1576384721";
        else if(id.equals("414514")) //Eldrazi Horror 3/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/11d25bde-a303-4b06-a3e1-4ad642deae58.jpg?1562636737";
        else if(id.equals("414497"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/e/3e2011f0-a640-4579-bd67-1dfbc09b8c09.jpg?1576385037";
        else if(id.equals("414478"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/e/ee648500-a213-4aa4-a97c-b7223c11bebd.jpg?1576384915";
        else if(id.equals("414442"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/b/0b0eab47-af62-4ee8-99cf-a864fadade2d.jpg?1576384676";
        else if(id.equals("414358"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/e/1eb4ddf4-f695-412d-be80-b93392432498.jpg?1576384142";
        else if(id.equals("414408"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/5/25baac6c-5bb4-4ecc-b1d5-fced52087bd9.jpg?1576384469";
        else if(id.equals("414465"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/8/f89f116a-1e8e-4ae7-be39-552e4954f229.jpg?1576384825";
        else if(id.equals("439843"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/9/397ba02d-f347-46f7-b028-dd4ba55faa2f.jpg?1572373698";
        else if(id.equals("439835"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/1/c16ba84e-a0cc-4c6c-9b80-713247b8fef9.jpg?1555040973";
        else if(id.equals("439825"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/6/66d9d524-3611-48d9-86c9-48e509e8ae70.jpg?1572373662";
        else if(id.equals("439839"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/0/303d51ab-b9c4-4647-950f-291daabe7b81.jpg?1555041001";
        else if(id.equals("439827"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/d/1d94ff37-f04e-48ee-8253-d62ab07f0632.jpg?1555428604";
        else if(id.equals("439816"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/e/8e7554bc-8583-4059-8895-c3845bc27ae3.jpg?1555428629";
        else if(id.equals("439819"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/8/d81c4b3f-81c2-403b-8a5d-c9415f73a1f9.jpg?1572373636";
        else if(id.equals("227290"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/7/57f0907f-74f4-4d86-93df-f2e50c9d0b2f.jpg?1562830557";
        else if(id.equals("244687"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/b/2b14ed17-1a35-4c49-ac46-3cad42d46c14.jpg?1562827887";
        else if(id.equals("222123"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/b/4b43b0cb-a5a3-47b4-9b6b-9d2638222bb6.jpg?1562829761";
        else if(id.equals("222906"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/4/e42a0a3d-a987-4b24-b9d4-27380a12e093.jpg?1562838647";
        else if(id.equals("227419"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/b/bb90a6f1-c7f2-4c2e-ab1e-59c5c7937841.jpg?1562836209";
        else if(id.equals("226755"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/1/11bf83bb-c95b-4b4f-9a56-ce7a1816307a.jpg?1562826346";
        else if(id.equals("221190"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/8/58ae9cbc-d88d-42df-ab76-63ab5d05c023.jpg?1562830610";
        else if(id.equals("222115"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/2/028aeebc-4073-4595-94da-02f9f96ea148.jpg?1562825445";
        else if(id.equals("222183"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/d/dd8ca448-f734-4cb9-b1d5-790eed9a4b2d.jpg?1562838270";
        else if(id.equals("222114"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/5/25b54a1d-e201-453b-9173-b04e06ee6fb7.jpg?1562827580";
        else if(id.equals("222117"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/1/6151cae7-92a4-4891-a952-21def412d3e4.jpg?1562831128";
        else if(id.equals("221222"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/8/f8b8f0b4-71e1-4822-99a1-b1b3c2f10cb2.jpg?1562839966";
        else if(id.equals("222107"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/d/cd5435d0-789f-4c42-8efc-165c072404a2.jpg?1562837238";
        else if(id.equals("221185"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/b/7bf864db-4754-433d-9d77-6695f78f6c09.jpg?1562832669";
        else if(id.equals("221173"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/b/ebf5e16f-a8bd-419f-b5ca-8c7fce09c4f1.jpg?1562839206";
        else if(id.equals("222108"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/3/8325c570-4d74-4e65-891c-3e153abf4bf9.jpg?1562833164";
        else if(id.equals("221215"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/8/88db324f-11f1-43d3-a897-f4e3caf8d642.jpg?1562833493";
        else if(id.equals("227090"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/c/ec00d2d2-6597-474a-9353-345bbedfe57e.jpg?1562839216";
        else if(id.equals("398442"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/f/9f25e1cf-eeb4-458d-8fb2-b3a2f86bdd54.jpg?1562033824";
        else if(id.equals("398423"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/0/b0d6caf0-4fa8-4ec5-b7f4-1307474d1b13.jpg?1562036951";
        else if(id.equals("398435"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/2/02d6d693-f1f3-4317-bcc0-c21fa8490d38.jpg?1590511929";
        else if(id.equals("398429"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/8/58c39df6-b237-40d1-bdcb-2fe5d05392a9.jpg?1562021001";
        else if(id.equals("439454"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/b/cb3587b9-e727-4f37-b4d6-1baa7316262f.jpg?1562937945";
        else if(id.equals("435451"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b6c78fee-c186-4209-8533-edd695b9836a.jpg?1562562746";
        else if (id.equals("1389"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/0/30345500-d430-4280-bfe3-de297309f136.jpg?1559597102";
        else if (id.equals("1390"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/a/5a240d1b-8430-4986-850d-32afa0e812b2.jpg?1559596752";
        else if (id.equals("1391"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/b/1b0f41e8-cf27-489b-812a-d566a75cf7f7.jpg?1559596849";
        else if (id.equals("2381"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/c/0c5c9379-b686-4823-b85a-eaf2c4b63205.jpg?1559603770";
        else if (id.equals("2382"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/0/10478e22-d1dd-4e02-81a7-d93ce71ed81d.jpg?1559604101";
        else if (id.equals("2383"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/0/50352268-88a6-4575-a5e1-cd8bef7f8286.jpg?1559603921";
        else if (id.equals("414789"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/e/2ef981a9-303e-4313-9265-77cc60323091.jpg?1562229658";
        else if (id.equals("414790"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/3/03e82924-899c-47b4-862a-7a27a96e285a.jpg?1562229261";
        else if (id.equals("414791"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/5/f57958e2-1e8f-48fa-816d-748ea2c7cb4e.jpg?1562230178";
        else if (id.equals("414792"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/f/3f89288a-9958-45c6-9bd2-24e6b3935171.jpg?1562229665";
        else if (id.equals("414793"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/5/85b37484-037a-497a-9820-97299d624daa.jpg?1562229691";
        else if (id.equals("205309"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/9/09e222f9-b7fc-49f0-8cef-9899aa333ecf.jpg?1562841209";
        else if (id.equals("205434"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/3/b3b6ad3d-a4d6-4ce9-bc0d-58fd83f83094.jpg?1562842861";
        else if (id.equals("205442"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/9/e9956850-0674-44e1-80e8-3875ef76d512.jpg?1562843350";
        else if (id.equals("205443"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/3/e3b5964a-78d8-453f-8cba-6ab01804054e.jpg?1562843341";
        else if (id.equals("205446"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/2/a262d93b-f95c-406c-9e54-cbd3ad14282f.jpg?1562842650";
        else if (id.equals("2743"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/6/4695653a-5c4c-4ff3-b80c-f4b6c685f370.jpg?1562907887";
        else if (id.equals("2744"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/a/6a90b49f-53b3-4ce0-92c1-bcd76d6981ea.jpg?1562914756";
        else if (id.equals("2745"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/d/ddca7e2e-bb0a-47ed-ade3-31900da992dc.jpg?1562936375";
        else if (id.equals("157871"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/3/4324380c-68b8-4955-ad92-76f921e6ffc1.jpg?1562829356";
        else if (id.equals("157886"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/a/3a310639-99ca-4a7e-9f65-731779f3ea46.jpg?1562828916";
        else if (id.equals("157889"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/e/1ee0be63-ec99-4291-b504-e17061c15a67.jpg?1562827639";
        else if (id.equals("158239"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/1/a1d2dedf-d0d8-42c5-a498-31e172a1b503.jpg?1562834034";
        else if (id.equals("2110"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/9/398a2b0f-0b91-408c-8083-3bc89873b69f.jpg?1559603803";
        else if (id.equals("2101"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/9/398a2b0f-0b91-408c-8083-3bc89873b69f.jpg?1559603803";
        else if (id.equals("3900"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/a/9ac60e8c-ef5b-4893-b3e5-4a54cb0a0d3a.jpg?1562592795";
        else if (id.equals("3981"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/f/4fa6c0d6-aa18-4c32-a641-1ec8e50a26f3.jpg?1562590659";
        else if (id.equals("426920"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/1/517b32e4-4b34-431f-8f3b-98a6cffc245a.jpg?1549941725";
        else if (id.equals("426915"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/e/eeac671f-2606-43ed-ad60-a69df5c150f6.jpg?1549941631";
        else if (id.equals("426914"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/4/a4b32135-7061-4278-a01a-4fcbaadc9706.jpg?1549941342";
        else if (id.equals("426917"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/c/9c6f5433-57cc-4cb3-8621-2575fcbff392.jpg?1549941629";
        else if (id.equals("426917t"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/d/2d1446ed-f114-421d-bb60-9aeb655e5adb.jpg?1562844787";
        else if (id.equals("426916"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/4/a47070a0-fd05-4ed9-a175-847a864478da.jpg?1549941630";
        else if (id.equals("426916t"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/a/1aea5e0b-dc4e-4055-9e13-1dfbc25a2f00.jpg?1562844782";
        else if (id.equals("47316011t"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/9/c994ea90-71f4-403f-9418-2b72cc2de14d.jpg?1569150300";
        else if (id.equals("47316012t"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/b/db951f76-b785-453e-91b9-b3b8a5c1cfd4.jpg?1569150303";
        else if (id.equals("47316013t"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/d/cd3ca6d5-4b2c-46d4-95f3-f0f2fa47f447.jpg?1569150305";
        else if (id.equals("426913"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/6/06c9e2e8-2b4c-4087-9141-6aa25a506626.jpg?1549941334";
        else if (id.equals("426912"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/3/937dbc51-b589-4237-9fce-ea5c757f7c48.jpg?1549941330";
        else if (id.equals("426919"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/c/5cf5c549-1e2a-4c47-baf7-e608661b3088.jpg?1549941724";
        else if (id.equals("426918"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/2/d2f3035c-ca27-40f3-ad73-c4e54bb2bcd7.jpg?1549941722";
        else if (id.equals("426926"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/e/fe1a4032-efbb-4f72-9181-994b2b35f598.jpg?1549941957";
        else if (id.equals("426925"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/6/c6f61e2b-e93b-4dda-95cf-9d0ff198c0a6.jpg?1549941949";
        else if (id.equals("426922"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/5/f59ea6f6-2dff-4e58-9166-57cac03f1d0a.jpg?1549941875";
        else if (id.equals("426921"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/4/6431d464-1f2b-42c4-ad38-67b7d0984080.jpg?1549941868";
        else if (id.equals("426924"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/1/11d84618-aca9-47dc-ae73-36a2c29f584c.jpg?1549941948";
        else if (id.equals("426923"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/9/b9623c8c-01b4-4e8f-a5b9-eeea408ec027.jpg?1549941877";
        else if (id.equals("3082"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/5/b5afe9b5-3be8-472a-95c3-2c34231bc042.jpg?1562770153";
        else if (id.equals("3083"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/5/b5afe9b5-3be8-472a-95c3-2c34231bc042.jpg?1562770153";
        else if (id.equals("3222"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/4/44be2d66-359e-4cc1-9670-119cb9c7d5f5.jpg?1562768261";
        else if (id.equals("3223"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/9/f9b0164c-2d4e-48ab-addd-322d9b504739.jpg?1562770860";
        else if (id.equals("912"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/c/fcc1004f-7cee-420a-9f0e-2986ed3ab852.jpg?1562942644";
        else if (id.equals("915"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/4/c4b610d3-2005-4347-bcda-c30b5b7972e5.jpg?1562931818";
        else if (id.equals("921"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/f/5f46783a-b91e-4829-a173-5515b09ca615.jpg?1562912566";
        else if (id.equals("922"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/1/31bf3f14-b5df-498b-a1bb-965885c82401.jpg?1562904228";
        else if (id.equals("923"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/8/18607bf6-ce11-41cb-b001-0c9538406ba0.jpg?1562899601";
        else if (id.equals("929"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/1/414d3cae-b8cf-4d53-bd6b-1aa83a828ba9.jpg?1562906979";
        else if (id.equals("946"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/9/f9d613d5-36a2-4633-b5af-64511bb29cc2.jpg?1562941972";
        else if (id.equals("947"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/0/c0b10fb7-8667-42bf-aeb6-35767a82917b.jpg?1562930986";
        else if (id.equals("74476"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/8/28f72260-c8f9-4c44-92b5-23cef6690fdd.jpg?1562876119";
        else if (id.equals("74489"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/a/ca03131a-9bd4-4fba-b95c-90f1831e86e7.jpg?1562879774";
        else if (id.equals("74536"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/3/73636ca0-2309-4bb3-9300-8bd0c0bb5b31.jpg?1562877808";
        else if (id.equals("74093"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/5/758abd53-6ad2-406e-8615-8e48678405b4.jpg?1562877848";
        else if (id.equals("74671"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/f/1fe2b76f-ddb7-49d5-933b-ccb06be5d46f.jpg?1562875903";
        else if (id.equals("376399"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/e/cec89c38-0b72-44b0-ac6c-7eb9503e1256.jpg?1562938742";
        else if (id.equals("451089") || id.equals("45108910"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/8/58164521-aeec-43fc-9db9-d595432dea6f.jpg?1564694999";
        else if (id.equals("451089t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/5/c5ad13b4-bbf5-4c98-868f-4d105eaf8833.jpg?1592710082";
        else if (id.equals("470745"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/a/ca4caa4e-6b8f-4be8-b177-de2ebe2c9201.jpg?1567044873";
        else if (id.equals("470609"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/9/e9d5aee0-5963-41db-a22b-cfea40a967a3.jpg?1567044805";
        else if (id.equals("470738"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/e/fe3b32dc-f7e6-455c-b9d1-c7f8d6259179.jpg?1567044854";
        else if (id.equals("78686"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/3/33a8e5b9-6bfb-4ff2-a16d-3168a5412807.jpg?1562758927";
        else if (id.equals("78688"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/3/b3523b8e-065f-427c-8d5b-eb731ca91ede.jpg?1562763691";
        else if (id.equals("78687"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/9/49999b95-5e62-414c-b975-4191b9c1ab39.jpg?1562759856";
        else if (id.equals("75291"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/8/98d3bc63-8814-46e7-a6ee-dd5b94a8257e.jpg?1562762956";
        else if (id.equals("78679"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/8/c8265c39-d287-4c5a-baba-f2f09dd80a1c.jpg?1562764226";
        else if (id.equals("78678"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/7/77ffd913-8efa-48e5-a5cf-293d3068dbbf.jpg?1562761560";
        else if (id.equals("78691"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/d/9de1eebf-5725-438c-bcf0-f3a4d8a89fb0.jpg?1562762993";
        else if (id.equals("78695"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/2/f2ddf1a3-e6fa-4dd0-b80d-1a585b51b934.jpg?1562765664";
        else if (id.equals("78694"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/e/6ee6cd34-c117-4d7e-97d1-8f8464bfaac8.jpg?1562761096";
        else if (id.equals("78600"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/6/864ad989-19a6-4930-8efc-bbc077a18c32.jpg?1562762069";
        else if (id.equals("78975"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/4/a4f4aa3b-c64a-4430-b1a2-a7fca87d0a22.jpg?1562763433";
        else if (id.equals("2832"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/5/85bcd723-780b-45ca-9476-d28270350013.jpg?1562922034";
        else if (id.equals("2802"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/f/bfc43585-55ac-4d58-9e80-b19a7c8c8662.jpg?1562933573";
        else if (id.equals("446807"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/0/a00a7180-49bd-4ead-852a-67b6b5e4b933.jpg?1564694995";
        else if (id.equals("247175"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/d/fd9920a0-78c2-4cc8-82e6-ea3a1e23b314.jpg?1562942793";
        else if (id.equals("247182"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/1/91a2217c-8478-479b-a146-2d78f407a36f.jpg?1562922037";
        else if (id.equals("122075"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/5/5526c510-bd33-4fac-8941-f19bd0997557.jpg?1562183342";
        else if (id.equals("121236"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/5/1566c8a2-aaca-4ce0-a36b-620ea6f135cb.jpg?1562177467";
        else if (id.equals("244724"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/b/cb09041b-4d09-4cae-9e85-b859edae885b.jpg?1562942950";
        else if (id.equals("262675"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/2/a2c044c0-3625-4bdf-9445-b462394cecae.jpg?1562933422";
        else if (id.equals("226735"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/d/9d9c1c46-7aa7-464c-87b0-b29b9663daef.jpg?1562932220";
        else if (id.equals("253433"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/1/b150d71f-11c9-40d6-a461-4967ef437315.jpg?1562936877";
        else if (id.equals("226721"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/d/9d9c1c46-7aa7-464c-87b0-b29b9663daef.jpg?1562932220";
        else if (id.equals("227417"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/a/6aef77b3-4b38-4902-9f7a-dc18b5bb9da9.jpg?1562920184";
        else if (id.equals("243229"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/c/7c5a3c09-5656-4975-ba03-2d809903ed18.jpg?1562924292";
        else if (id.equals("242537"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/3/932d753d-9584-4ad8-9a5e-a3524184f961.jpg?1562929672";
        else if (id.equals("253426"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/3/1303e02a-ef69-4817-bca5-02c74774b811.jpg?1562899503";
        else if (id.equals("262875"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/a/aae6fb12-b252-453b-bca7-1ea2a0d6c8dc.jpg?1562935354";
        else if (id.equals("222178"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/5/f500cb95-d5ea-4cf2-920a-f1df45a9059b.jpg?1562953084";
        else if (id.equals("249422"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/0/e00ae92c-af6d-4a00-b102-c6d3bcc394b4.jpg?1562948371";
        else if (id.equals("247125"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/6/b6edac85-78e7-4e90-b538-b67c88bb5c62.jpg?1562938113";
        else if (id.equals("262694"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/f/6f35e364-81d9-4888-993b-acc7a53d963c.jpg?1562921188";
        else if (id.equals("244740"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/8/683af377-c491-4f62-900c-6b83d75c33c9.jpg?1562919527";
        else if (id.equals("262699"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/a/aae6fb12-b252-453b-bca7-1ea2a0d6c8dc.jpg?1562935354";
        else if (id.equals("262857"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/8/9831e3cc-659b-4408-b5d8-a27ae2738680.jpg?1562930830";
        else if (id.equals("414499"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/7/078b2103-15ce-456d-b092-352fa7222935.jpg?1562723962";
        else if (id.equals("414496"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/e/3e2011f0-a640-4579-bd67-1dfbc09b8c09.jpg?1562731266";
        else if (id.equals("414448"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/6/460f7733-c0a6-4439-a313-7b26ae6ee15b.jpg?1562732302";
        else if (id.equals("414346"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/2/22e816af-df55-4a3f-a6e7-0ff3bb1b45b5.jpg?1540920747";
        else if (id.equals("414464"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/8/f89f116a-1e8e-4ae7-be39-552e4954f229.jpg?1562756276";
        else if (id.equals("414349"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/0/30c3d4c1-dc3d-4529-9d6e-8c16149cf6da.jpg?1562729197";
        else if (id.equals("414470"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/6/a63c30c0-369a-4a75-b352-edab4d263d1b.jpg?1562745465";
        else if (id.equals("414350"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/0/30c3d4c1-dc3d-4529-9d6e-8c16149cf6da.jpg?1562729197";
        else if (id.equals("414357"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/e/1eb4ddf4-f695-412d-be80-b93392432498.jpg?1562726998";
        else if (id.equals("414479"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/d/0dbaef61-fa39-4ea7-bc21-445401c373e7.jpg?1562724612";
        else if (id.equals("414477"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/e/ee648500-a213-4aa4-a97c-b7223c11bebd.jpg?1562754423";
        else if (id.equals("414407"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/5/25baac6c-5bb4-4ecc-b1d5-fced52087bd9.jpg?1562727704";
        else if (id.equals("414421"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/f/7f95145a-41a1-478e-bf8a-ea8838d6f9b1.jpg?1562740440";
        else if (id.equals("414429"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/9/0900e494-962d-48c6-8e78-66a489be4bb2.jpg?1562724107";
        else if (id.equals("414304"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/7/27907985-b5f6-4098-ab43-15a0c2bf94d5.jpg?1562728142";
        else if (id.equals("414313"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/6/b6867ddd-f953-41c6-ba36-86ae2c14c908.jpg?1562747201";
        else if (id.equals("414314"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/6/b6867ddd-f953-41c6-ba36-86ae2c14c908.jpg?1562747201";
        else if (id.equals("414319"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/7/c75c035a-7da9-4b36-982d-fca8220b1797.jpg?1562749301";
        else if (id.equals("414324"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/a/9a55b60a-5d90-4f73-984e-53fdcc0366e4.jpg?1562744017";
        else if (id.equals("414441"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/b/0b0eab47-af62-4ee8-99cf-a864fadade2d.jpg?1562724176";
        else if (id.equals("456235"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/1/01ce2601-ae94-4ab5-bbd2-65f47281ca28.jpg?1544060145";
        else if (id.equals("452980"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/4/44614c6d-5508-4077-b825-66d5d684086c.jpg?1557465654";
        else if (id.equals("452979"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/2/92162888-35ea-4f4f-ab99-64dd3104e230.jpg?1557465657";
        else if (id.equals("452977"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/a/4a82084e-b178-442b-8007-7b2a70f3fbba.jpg?1557465653";
        else if (id.equals("452978"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/5/054a4e4f-8baa-41cf-b24c-d068e8b9a070.jpg?1557465656";
        else if (id.equals("452975"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/e/1e4e9e35-6cbc-4997-beff-d1a22d87545e.jpg?1557465652";
        else if (id.equals("452976"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/e/feb4b39f-d309-49ba-b427-240b7fdc1099.jpg?1557465650";
        else if (id.equals("452973"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/c/ace631d1-897a-417e-8628-0170713f03d3.jpg?1557465649";
        else if (id.equals("452974"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/0/e0644c92-4d67-475e-8c8e-0e2c493682fb.jpg?1557465652";
        else if (id.equals("452971"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/d/ad454e7a-06c9-4694-ae68-7b1431e00077.jpg?1557465646";
        else if (id.equals("452972"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/9/890ac54c-6fd7-4e46-8ce4-8926c6975f60.jpg?1557465648";
        else if (id.equals("430840"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/6/76f21f0b-aaa5-4677-8398-cef98c6fac2a.jpg?1562803878";
        else if (id.equals("430842"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/9/f928e8e8-aa20-402c-85bd-59106e9b9cc7.jpg?1562820622";
        else if (id.equals("430841"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/c/2c25b8ef-6331-49df-9457-b8b4e44da2c9.jpg?1562793920";
        else if (id.equals("430844"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/3/0383401f-d453-4e8f-82d2-5c016acc2591.jpg?1562787667";
        else if (id.equals("430843"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/c/1ca644e3-4fb3-4d38-b714-e3d7459bd8b9.jpg?1562791344";
        else if (id.equals("430846"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/7/7713ba59-dd4c-4b49-93a7-292728df86b8.jpg?1562803886";
        else if (id.equals("430845"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/5/054b07d8-99ae-430b-8e54-f9601fa572e7.jpg?1562787788";
        else if (id.equals("430837"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/9/d998db65-8785-4ee9-940e-fa9ab62e180f.jpg?1562816967";
        else if (id.equals("430839"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/4/0468e488-94ce-4ae3-abe4-7782673a7e62.jpg?1562787748";
        else if (id.equals("430838"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/c/1c1ead90-10d8-4217-80e4-6f40320c5569.jpg?1562791309";
        else if (id.equals("2470"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/f/af976f42-3d56-4e32-8294-970a276a4bf3.jpg?1562927660";
        else if (id.equals("2469"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/d/3d0006f6-2f96-453d-9145-eaefa588efbc.jpg?1562906229";
        else if (id.equals("2466"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/5/75b67eb2-b60e-46b4-9d48-11c284957bec.jpg?1562916780";
        else if (id.equals("2480"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/1/f16df768-06de-43a0-b548-44fb0887490b.jpg?1562940406";
        else if (id.equals("2635"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/8/7880e815-53e7-43e0-befd-e368f00a75d8.jpg?1562917281";
        else if (id.equals("221209"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/b/7bf864db-4754-433d-9d77-6695f78f6c09.jpg?1562832669";
        else if (id.equals("227415"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/b/bb90a6f1-c7f2-4c2e-ab1e-59c5c7937841.jpg?1562836209";
        else if (id.equals("221211"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/8/88db324f-11f1-43d3-a897-f4e3caf8d642.jpg?1562833493";
        else if (id.equals("221212"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/8/f8b8f0b4-71e1-4822-99a1-b1b3c2f10cb2.jpg?1562839966";
        else if (id.equals("244683"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/b/2b14ed17-1a35-4c49-ac46-3cad42d46c14.jpg?1562827887";
        else if (id.equals("222915"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/4/e42a0a3d-a987-4b24-b9d4-27380a12e093.jpg?1562838647";
        else if (id.equals("222112"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/d/cd5435d0-789f-4c42-8efc-165c072404a2.jpg?1562837238";
        else if (id.equals("222118"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/5/25b54a1d-e201-453b-9173-b04e06ee6fb7.jpg?1562827580";
        else if (id.equals("222105"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/3/8325c570-4d74-4e65-891c-3e153abf4bf9.jpg?1562833164";
        else if (id.equals("222111"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/2/028aeebc-4073-4595-94da-02f9f96ea148.jpg?1562825445";
        else if (id.equals("222016"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/8/58ae9cbc-d88d-42df-ab76-63ab5d05c023.jpg?1562830610";
        else if (id.equals("222124"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/b/4b43b0cb-a5a3-47b4-9b6b-9d2638222bb6.jpg?1562829761";
        else if (id.equals("226749"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/1/11bf83bb-c95b-4b4f-9a56-ce7a1816307a.jpg?1562826346";
        else if (id.equals("221179"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/b/ebf5e16f-a8bd-419f-b5ca-8c7fce09c4f1.jpg?1562839206";
        else if (id.equals("245251"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/4/b4160322-ff40-41a4-887a-73cd6b85ae45.jpg?1562835830";
        else if (id.equals("245250"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/4/b4160322-ff40-41a4-887a-73cd6b85ae45.jpg?1562835830";
        else if (id.equals("222186"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/1/6151cae7-92a4-4891-a952-21def412d3e4.jpg?1562831128";
        else if (id.equals("227072"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/3/13896468-e3d0-4bcb-b09e-b5c187aecb03.jpg?1562826506";
        else if (id.equals("227061"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/3/13896468-e3d0-4bcb-b09e-b5c187aecb03.jpg?1562826506";
        else if (id.equals("227409"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/7/57f0907f-74f4-4d86-93df-f2e50c9d0b2f.jpg?1562830557";
        else if (id.equals("222189"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/d/dd8ca448-f734-4cb9-b1d5-790eed9a4b2d.jpg?1562838270";
        else if (id.equals("227084"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/c/ec00d2d2-6597-474a-9353-345bbedfe57e.jpg?1562839216";
        else if (id.equals("447354"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/b/7b215968-93a6-4278-ac61-4e3e8c3c3943.jpg?1566971561";
        else if (id.equals("447355"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/b/7b215968-93a6-4278-ac61-4e3e8c3c3943.jpg?1566971561";
        else if (id.equals("184714"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/7/1777f69c-869e-414e-afe3-892714a6032a.jpg?1562867836";
        else if (id.equals("202605"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/f/5f6529eb-79ff-4ddc-9fae-38326324f7e6.jpg?1562917476";
        else if (id.equals("202443"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/8/082cf845-5a24-4f00-bad2-a3d0d07f59e6.jpg?1562896910";
        else if (id.equals("398438"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/f/ff0063da-ab6b-499d-8e87-8b34d46f0372.jpg?1562209457";
        else if (id.equals("398432"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/f/ff0063da-ab6b-499d-8e87-8b34d46f0372.jpg?1562209457";
        else if (id.equals("398434"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/2/02d6d693-f1f3-4317-bcc0-c21fa8490d38.jpg?1562005031";
        else if (id.equals("398441"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/f/9f25e1cf-eeb4-458d-8fb2-b3a2f86bdd54.jpg?1562033824";
        else if (id.equals("398422"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/0/b0d6caf0-4fa8-4ec5-b7f4-1307474d1b13.jpg?1562036951";
        else if (id.equals("398428"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/8/58c39df6-b237-40d1-bdcb-2fe5d05392a9.jpg?1562021001";
        else if (id.equals("6528"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/d/ade6a71a-e8ec-4d41-8a39-3eacf0097c8b.jpg?1562936067";
        else if (id.equals("4259"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/c/7c93d4e9-7fd6-4814-b86b-89b92d1dad3b.jpg?1562446874";
        else if (id.equals("439824"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/6/66d9d524-3611-48d9-86c9-48e509e8ae70.jpg?1555428581";
        else if (id.equals("439826"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/d/1d94ff37-f04e-48ee-8253-d62ab07f0632.jpg?1555428604";
        else if (id.equals("439834"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/1/c16ba84e-a0cc-4c6c-9b80-713247b8fef9.jpg?1555040973";
        else if (id.equals("439818"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/8/d81c4b3f-81c2-403b-8a5d-c9415f73a1f9.jpg?1555040854";
        else if (id.equals("439815"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/e/8e7554bc-8583-4059-8895-c3845bc27ae3.jpg?1555428629";
        else if (id.equals("439838"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/0/303d51ab-b9c4-4647-950f-291daabe7b81.jpg?1555041001";
        else if (id.equals("439842"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/9/397ba02d-f347-46f7-b028-dd4ba55faa2f.jpg?1555427909";
        else if (id.equals("457365"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/5/b5873efa-d573-4435-81ad-48df2ca5c7f2.jpg?1551138454";
        else if (id.equals("457366"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/1/d1dbc559-c78c-4675-9582-9c28f8151bc7.jpg?1549415048";
        else if (id.equals("457367"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/b/9bd15da6-2b86-4dba-951d-318c7d9a5dde.jpg?1549415053";
        else if (id.equals("457368"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/0/00320106-ce51-46a9-b0f9-79b3baf4e505.jpg?1549415058";
        else if (id.equals("457369"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/b/ab0ba4ef-9e82-4177-a80f-8fa6f6a5bd60.jpg?1549416398";
        else if (id.equals("457370"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/7/075bbe5d-d0f3-4be3-a3a6-072d5d3d614c.jpg?1549414568";
        else if (id.equals("457371"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/6/f6200937-3146-4972-ab83-051ade3b7a52.jpg?1551138470";
        else if (id.equals("457372"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/0/50ae0831-f3ba-4535-bfb6-feefbbc15275.jpg?1551138459";
        else if (id.equals("457373"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/e/2eefd8c1-96ce-4d7a-8aaf-29c35d634dda.jpg?1551138529";
        else if (id.equals("457374"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/0/0070651d-79aa-4ea6-b703-6ecd3528b548.jpg?1551138527";
        else if (id.equals("1158"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/3/c3591170-645f-4645-bc39-b90b7b6ddac7.jpg?1559597137";
        else if (id.equals("409826"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/e/6e099a6a-97c4-42cd-aca6-5e1a2da0d5e5.jpg?1576384210";
        else if (id.equals("409899"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/8/f8bdc165-4c6f-47e6-8bda-877c0be3613b.jpg?1576384673";
        else if (id.equals("84716"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/4/84920a21-ee2a-41ac-a369-347633d10371.jpg?1562494702";
        else if (id.equals("87600"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/2/42ba0e13-d20f-47f9-9c86-2b0b13c39ada.jpg?1562493487";
        else if (id.equals("87599"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/b/0b61d772-2d8b-4acf-9dd2-b2e8b03538c8.jpg?1562492461";
        else if (id.equals("87595"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/2/d224c50f-8146-4c91-9401-04e5bd306d02.jpg?1562496100";
        else if (id.equals("87596"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/1/41004bdf-8e09-4b2c-9e9c-26c25eac9854.jpg?1562493483";
        else if (id.equals("106631"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/c/ac2e32d0-f172-4934-9d73-1bc2ab86586e.jpg?1562781784";
        else if (id.equals("9668"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/7/17c18690-cf8c-4006-a981-6258d18ba538.jpg?1562799066";
        else if (id.equals("9749"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/f/3fcefcab-8988-47e8-89bb-9b76f14c9d8b.jpg?1562799089";
        else if (id.equals("9780"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/9/a9f9c279-e382-4feb-9575-196e7cf5d7dc.jpg?1562799139";
        else if (id.equals("9844"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/9/a9f9c279-e382-4feb-9575-196e7cf5d7dc.jpg?1562799139";
        else if (id.equals("456821"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/6/468d5308-2a6c-440e-a8d0-1c5e084afb82.jpg?1547517180";
        else if (id.equals("74358"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/9/8987644d-5a31-4a4e-9a8a-3d6260ed0fd6.jpg?1562488870";
        else if (id.equals("73956"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/0/c01e8089-c3a9-413b-ae2d-39ede87516d3.jpg?1562489378";
        else if (id.equals("74242"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/5/85cbebbb-7ea4-4140-933f-186cad08697d.jpg?1562488867";
        else if (id.equals("74322"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/9/49dd5a66-101d-4f88-b1ba-e2368203d408.jpg?1562488377";
        else if (id.equals("4429"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/8/3884bede-df28-42e8-9ac9-ae03118b1985.jpg?1562800239";
        else if (id.equals("113522"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/8/b8a3cdfe-0289-474b-b9c4-07e8c6588ec5.jpg?1562933997";
        else if (id.equals("51733"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/7/27118cbb-a386-4145-8716-961ed0f653bf.jpg?1562902951";
        else if (id.equals("52362"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/b/9bd7a7f1-2221-4565-8c6e-1815def3bd2c.jpg?1562546811";
        else if (id.equals("52415"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/8/8825493a-878d-4df3-8d7a-98518358d678.jpg?1562546240";
        else if(id.equals("53214t"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/4/1449862b-309e-4c58-ac94-13d1acdd363f.jpg?1562541935";
        else if(id.equals("53179t"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/9/d9623e74-3b94-4842-903f-ed52931bdf6a.jpg?1562636919";
        else if(id.equals("16806"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/1/f1bb8fb5-32f2-444d-85cb-de84657b21bd.jpg?1561758404";
        else if(id.equals("16807"))
            cardurl = "https://img.scryfall.com/cards/large/back/f/1/f1bb8fb5-32f2-444d-85cb-de84657b21bd.jpg?1561758404";
        else if(id.equals("16808"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/e/2eb08fc5-29a4-4911-ac94-dc5ff2fc2ace.jpg?1561756860";
        else if(id.equals("16809"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/e/9e5180da-d757-415c-b92d-090ad5c1b658.jpg?1561757695";
        else if(id.equals("16809t"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/e/8ee8b915-afd3-4fad-8aef-7e9cbbbbc2e4.jpg?1561757559";
        else if(id.equals("16751"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/9/39a89c44-1aa7-4f2e-909b-d821ec2b7948.jpg?1561756358";
        else if(id.equals("17639t"))
            cardurl = "https://img.scryfall.com/cards/large/back/8/c/8ce60642-e207-46e6-b198-d803ff3b47f4.jpg?1562921132";
        else if(id.equals("16740t") || id.equals("294023t")) //Gremlin 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/6/c6071fed-39c1-4f3b-a821-1611aedd8054.jpg?1561758029";
        else if (id.equals("53143t") || id.equals("17717t") || id.equals("17705t") || id.equals("17669t") || id.equals("17661t")
                || id.equals("17645t") || id.equals("17573t") || id.equals("17549t") || id.equals("17537t") || id.equals("17513t")
                || id.equals("17429t") || id.equals("17417t") || id.equals("17405t") || id.equals("17393t") || id.equals("17285t")
                || id.equals("17273t") || id.equals("17249t") || id.equals("17141t") || id.equals("17129t") || id.equals("17117t")
                || id.equals("17105t") || id.equals("17093t") || id.equals("17081t") || id.equals("17866t") || id.equals("294460t")
                || id.equals("11492115t") || id.equals("209162t") || id.equals("17010t") || id.equals("16997t")) //Saproling 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/4/34f6ffaa-6dee-49db-ac59-745eae5e75b2.jpg?1562702017";
        else if(id.endsWith("53141t")) //Elf Druid 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/458f44dd-83f1-497e-b5d0-e3417eb9dfec.jpg?1592672533";
        else if(id.equals("53134t") || id.equals("54047313t")) //Beast 4/4
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/6/06b5e4d2-7eac-4ee9-82aa-80a668705679.jpg?1625974919";
        else if(id.equals("16981t") || id.equals("16978t") || id.equals("16967t") || id.equals("17841t")
                || id.equals("17850t") || id.equals("17852t")) // Elf Warrior 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/118d0655-5719-4512-8bc1-fe759669811b.jpg?1615686731";
        else if (id.equals("16975t") || id.equals("17848t") || id.equals("53054t") || id.equals("19784312t") || id.equals("29669412t") || 
                id.equals("53939512t")) // Wolf 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/462ff49b-a004-4dab-a25b-65cb18c1bbec.jpg?1592672584";
        else if (id.equals("16933t") || id.equals("476107t")) //Dragon 5/5
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/9/993b3b90-74c3-479b-b3e6-3f1cd8f1da04.jpg?1561757651";
        else if(id.equals("16885t")) //Thopter blue 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a3506ee6-a168-49a4-9814-2858194be60e.jpg?1592710025";
        else if(id.equals("16847t")) //Angel 4/4
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/6/1671013a-2c15-44f0-b4bc-057eb5f727db.jpg?1562701916";
        else if(id.equals("17656t") || id.equals("17500t") || id.equals("17080t")) //Elemental 3/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/a/da6283ba-1297-4c7d-8744-f530c04194cd.jpg?1561756395";
        else if (id.equals("17501t") || id.equals("17494t") || id.equals("17354t") || id.equals("17062t")) //Spirit 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/3/b3c9a097-219b-4aaf-831f-cc0cddbcfaae.jpg?1561757870";
        else if (id.equals("17493t")) //Bear 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/8/c879d4a6-cef5-48f1-8c08-f5b59ec850de.jpg?1562857282";
        else if (id.equals("473117t")) //Bear 2/2 ELD
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/0/b0f09f9e-e0f9-4ed8-bfc0-5f1a3046106e.jpg?1572489163";
        else if (id.equals("17358t") || id.equals("54047312t")) //Beast 3/3
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/f/3fc3a29a-280d-4f2c-9a01-8cfead75f583.jpg?1561756988";
        else if(id.equals("17207t")) //Sliver 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/e/dec96e95-5580-4110-86ec-561007ab0f1e.jpg?1562640084";
        else if(id.equals("17071t")) //Cat Warrior 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/9/29c4e4f2-0040-4490-b357-660d729ad9cc.jpg?1562636772";
        else if(id.equals("17069t")) //Voja 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/8/2879010f-b752-4808-8531-d24e612de0d9.jpg?1541006575";
        else if(id.equals("17060t") || id.equals("476037t") || id.equals("473092t") || id.equals("473062t")) //Rat 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/a/1a85fe9d-ef18-46c4-88b0-cf2e222e30e4.jpg?1562279130";
        else if(id.equals("17061t"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/c/acd51eed-bd5a-417a-811d-fbd1c08a3715.jpg?1561757812";
        else if(id.equals("17955"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/8/b86ac828-7b49-4663-a718-99fcac904568.jpg?1561756381";
        else if(id.equals("476097t") || id.equals("293685t") || id.equals("293652t") || id.equals("296820t") ) //Zombie 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1562844819";
        else if(id.equals("999901t")) //Monarch Token
            cardurl = "https://img.scryfall.com/cards/large/front/4/0/40b79918-22a7-4fff-82a6-8ebfe6e87185.jpg?1561897497";
        else if(id.equals("999902t")) //City's Blessing
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/a/ba64ed3e-93c5-406f-a38d-65cc68472122.jpg?1561757924";
        else if(id.equals("19462t") || id.equals("19463t") || id.equals("19464t") || id.equals("19465t"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/2/d2f51f4d-eb6d-4503-b9a4-559db1b9b16f.jpg?1574710411";
        else if(id.equals("19476t") || id.equals("19477t"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/4/340fb06f-4bb0-4d23-b08c-8b1da4a8c2ad.jpg?1574709457";
        else if(id.equals("159127"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/e/1e14cf3a-3c5a-4c22-88d1-1b19660b2e2a.jpg?1559592579";
        else if(id.equals("159130"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/4/f4c21c0d-91ee-4c2c-bfa4-81bb07106842.jpg?1559592507";
        else if(id.equals("159132"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/0/b03bc922-782b-4254-897c-90d202b4cda4.jpg?1559592285";
        else if(id.equals("159764"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/8/98f443cb-55bb-4e83-826a-98261287bfd3.jpg?1559592330";
        else if(id.equals("159832"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/b/0b5f694c-11da-41af-9997-0aff93619248.jpg?1559592387";
        else if(id.equals("159237"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/e/1e76a75a-7125-4957-ab7a-8e7ead21d002.jpg?1559592440";
        else if(id.equals("159136"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/a/fa740755-244f-4658-a9e2-aa4cf6742808.jpg?1559592290";
        else if(id.equals("294381t")) //Bear 4/4
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/a/ca3dae7d-3880-4c0a-acfb-8fd227cf9fab.jpg?1562640044";
        else if(id.equals("294366t")) //Elemental 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/d/5dc134da-51b8-452d-b515-54def56fe0c7.jpg?1604198535";
        else if (id.equals("294235t") || id.equals("293899t")) // Eldrazi Spawn 0/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/7/7787eae2-7dfb-44ab-8e92-56fdfc0bb39e.jpg?1593142790";
        else if(id.equals("293497t")) //Drake 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/c/dcd1cef8-d78a-4bdb-8da0-a50ad199c691.jpg?1625974651";
        else if(id.equals("476370"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/4/14b28eae-e8ed-4b99-b6ec-86d0716ec473.jpg?1581480007";
        else if(id.equals("479417"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/f/efcbd4ef-3bf4-4f22-9069-2a11c9619a43.jpg?1583965446";
        else if(id.equals("482713t"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/9/29d42a00-299d-47d3-ba03-e63812d57931.jpg?1586452636";
        else if(id.equals("4827131t"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/d/1dee8c94-cdc8-42b2-a393-0c0c8e439125.jpg?1586453065";
        else if(id.equals("484902t") || id.equals("484904t"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/2/720f3e68-84c0-462e-a0d1-90236ccc494a.jpg?1562539782";
        else if(id.equals("294690t"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/8/082c3bad-3fea-4c3f-8263-4b16139bb32a.jpg?1562701890";
        else if(id.equals("47963911t"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/9/f918b740-1984-4090-8886-9e290a698b95.jpg?1586451994";
        else if(id.equals("479634t"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/9/a9cc7c63-5d13-4fd6-af9d-4a26c2bab8e6.jpg?1588521003";
        else if(id.equals("485469t"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/3/4306be80-d7c9-4bcf-a3de-4bf159475546.jpg?1592323967";
        else if(id.equals("489663"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/6/d605c780-a42a-4816-8fb9-63e3114a8246.jpg?1592762067";
        else if(id.equals("48966310t"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/b/fbdf8dc1-1b10-4fce-97b9-1f5600500cc1.jpg?1592324494";
        else if(id.equals("48966311t"))
            cardurl = "https://img.scryfall.com/cards/large/front/4/f/4f8107b3-8539-4b9c-8d0d-c512c940838f.jpg?1592324480";
        else if(id.equals("489987t"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/b/fb248ba0-2ee7-4994-be57-2bcc8df29680.jpg?1596043510";
        else if(id.equals("489822t"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/7/c7e7822b-f155-4f3f-b835-ec64f3a71307.jpg?1596044257";
        else if(id.equals("489930t"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/9/791f5fa0-f972-455e-9802-ff299853607f.jpg?1596044240";
        else if(id.equals("491334"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/6/96d1a254-01a8-4590-8878-690c5bfb4a95.jpg?1596139673";
        else if(id.equals("491335"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/c/ec386bc3-137b-49b5-8380-8daff470f0bc.jpg?1596139680";
        else if(id.equals("491344"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/b/db149aaa-3da9-48c4-92cc-b3d804285290.jpg?1596139686";
        else if(id.equals("491345"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/2/32301593-f16a-4a46-8a4e-eecedd2a9013.jpg?1596139691";
        else if(id.equals("491346"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/b/eb49805c-8546-463d-b78c-f4ea109851b4.jpg?1596139696";
        else if(id.equals("491347"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/d/dd595e2f-65e4-46e8-9d28-f94ac308b275.jpg?1596139702";
        else if(id.equals("491348"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/9/f9baef6e-a086-41d4-a20e-486f01d72406.jpg?1596139709";
        else if(id.equals("491349"))
            cardurl = "https://img.scryfall.com/cards/large/front/e/c/ec136ce7-bad4-4ebb-ab00-b86de3d209a7.jpg?1596139714";
        else if(id.equals("491350"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/a/cacaf5ec-6745-4584-9175-36c98742958f.jpg?1596139721";
        else if(id.equals("491351"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/c/6c821158-f71a-48f9-b6b4-b0e605f22bec.jpg?1596278384";
        else if(id.equals("491352"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/9/b9a50516-a20f-4e6e-b4f2-0049b673f942.jpg?1596139732";
        else if(id.equals("491353"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/5/95702503-8f2d-46c8-abdb-6edd6c431d19.jpg?1596278403";
        else if(id.equals("491354"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/3/73731e45-51bb-4188-a54d-fdaa4bdfaf1f.jpg?1596139744";
        else if(id.equals("491355"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/3/d35575d0-0b10-4d1b-b5a2-a9f36f9eada4.jpg?1596139751";
        else if(id.equals("491356"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/2/62d2058c-3f20-4566-b366-93a2cbbe682f.jpg?1596139757";
        else if(id.equals("491357"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/b/5b8c11ba-533d-48c9-821c-3fec846bca97.jpg?1596139762";
        else if(id.equals("491358"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/8/187abedf-c2eb-453b-bea0-a10afa399e03.jpg?1596139769";
        else if(id.equals("491359"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/0/20c9c856-af15-40b1-a799-1c2066df2099.jpg?1596139775";
        else if(id.equals("491360"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/c/1c3fc61c-c26e-47f3-a1eb-f6f10f8469e2.jpg?1596139781";
        else if(id.equals("491361"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/0/8008977f-b164-4ab7-a38a-25b382c6a16f.jpg?1596139788";
        else if(id.equals("491362"))
            cardurl = "https://img.scryfall.com/cards/large/front/d/a/dac080ef-8f40-43a2-8440-b457b6074b69.jpg?1596139794";
        else if(id.equals("491363"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/6/86f670f9-c5b7-4eb0-a7d0-d16513fadf74.jpg?1596139800";
        else if(id.equals("491364"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/c/3c9847f3-5a4c-4b49-8e25-e444d1446bf9.jpg?1596139806";
        else if(id.equals("491365"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/a/2abc5ac8-b944-4b71-b022-c78183eb92c3.jpg?1596139812";
        else if(id.equals("491366"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/c/0ccd5597-2d4e-4f3e-94b7-46783486853a.jpg?1596139818";
        else if(id.equals("491367"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/e/0e94d334-e043-42f2-ba5c-be497d82f2c8.jpg?1596139824";
        else if(id.equals("491368"))
            cardurl = "https://img.scryfall.com/cards/large/front/8/b/8bc6178b-16e7-4089-974f-7048b9632fc2.jpg?1596139831";
        else if(id.equals("491369"))
            cardurl = "https://img.scryfall.com/cards/large/front/7/0/70eab734-875b-4b76-901b-3ac7d2133ad9.jpg?1596139837";
        else if(id.equals("491370"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/d/6d7cd274-ed83-475a-9b4f-adb9c780a6f4.jpg?1596139842";
        else if(id.equals("491371"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/4/547e3aa5-d88a-4418-ab9d-dd65385f031b.jpg?1596139849";
        else if(id.equals("491372"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/c/5cc0b4eb-8ee9-4213-8194-02e7d63428d3.jpg?1596139855";
        else if(id.equals("491373"))
            cardurl = "https://img.scryfall.com/cards/large/front/0/a/0a469d00-1416-48dd-ad91-eb6f3fb4b42b.jpg?1596139861";
        else if(id.equals("491374"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/2/22cd80dd-1c57-423c-81e2-9a956901565f.jpg?1596139867";
        else if(id.equals("491375"))
            cardurl = "https://img.scryfall.com/cards/large/front/3/a/3a195efe-8c4f-479d-bd0f-563ee4bb49a1.jpg?1596139873";
        else if(id.equals("491376"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/3/93c0681d-97da-4363-b75a-079c209e7e4a.jpg?1596139878";
        else if(id.equals("491377"))
            cardurl = "https://img.scryfall.com/cards/large/front/f/0/f097accb-28ad-4b22-b615-103c74e07708.jpg?1596139884";
        else if(id.equals("491378"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/9/69e55604-56da-44b5-aa78-f5de76ce9d20.jpg?1596139891";
        else if(id.equals("491379"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/4/546e0452-5304-41fa-9e3a-a3fa5a571315.jpg?1596139896";
        else if(id.equals("491380"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/3/936335d7-1c4a-4fcd-80ff-cd4d4fcab8c4.jpg?1596139903";
        else if(id.equals("491381"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/7/c75672e0-fa2d-43c5-9381-e17f2fd6d3bc.jpg?1596139909";
        else if(id.equals("491377t"))
            cardurl = "https://img.scryfall.com/cards/large/front/a/6/a6ee0db9-ac89-4ab6-ac2e-8a7527d9ecbd.jpg?1596045113";
        else if(id.equals("491372t"))
            cardurl = "https://img.scryfall.com/cards/large/front/c/f/cf371056-43dd-41ab-8d05-b16a8bdc8d28.jpg?1596045227";
        else if(id.equals("491365t"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/e/9ecc467e-b345-446c-b9b7-5f164e6651a4.jpg?1596043489";
        else if(id.equals("295116t") || id.equals("295103t"))
            cardurl = "https://img.scryfall.com/cards/large/front/2/d/2d1446ed-f114-421d-bb60-9aeb655e5adb.jpg?1562844787";
        else if(id.equals("295077t"))
            cardurl = "https://img.scryfall.com/cards/large/front/6/a/6aaa8539-8d21-4da1-8410-d4354078390f.jpg?1562844799";
        else if(id.equals("295041t"))
            cardurl = "https://img.scryfall.com/cards/large/front/1/a/1aea5e0b-dc4e-4055-9e13-1dfbc25a2f00.jpg?1562844782";
        else if(id.equals("294952t") || id.equals("294950t"))
            cardurl = "https://img.scryfall.com/cards/large/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1562844819";
        else if(id.equals("491633t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/1/91098481-46c2-49bf-8123-e9cab2f22b84.jpg?1600466796";
        else if(id.equals("491633"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/4/c470539a-9cc7-4175-8f7c-c982b6072b6d.jpg?1601064253";
        else if(id.equals("491634"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/4/c470539a-9cc7-4175-8f7c-c982b6072b6d.jpg?1601064253";
        else if(id.equals("491641"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1601064385";
        else if(id.equals("491642"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1601064385";
        else if(id.equals("491649"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/d/ada9a974-8f1f-4148-bd61-200fc14714b2.jpg?1601064484";
        else if(id.equals("491650"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/d/ada9a974-8f1f-4148-bd61-200fc14714b2.jpg?1601064484";
        else if(id.equals("491654"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b6e6be8c-41c3-4348-a8dd-b40ceb24e9b4.jpg?1601064504";
        else if(id.equals("491655"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/6/b6e6be8c-41c3-4348-a8dd-b40ceb24e9b4.jpg?1601064504";
        else if(id.equals("491662"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/2/f25d56f9-aa54-4657-9ac9-e93fbba3e715.jpg?1601064554";
        else if(id.equals("491663"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/2/f25d56f9-aa54-4657-9ac9-e93fbba3e715.jpg?1601064554";
        else if(id.equals("491666"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/1/014027c4-7f9d-4096-b308-ea4be574c0d4.jpg?1601064584";
        else if(id.equals("491667"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/1/014027c4-7f9d-4096-b308-ea4be574c0d4.jpg?1601064584";
        else if(id.equals("491673"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/f/5f411f08-45dd-4d73-8894-daf51c175150.jpg?1601064619";
        else if(id.equals("491674"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/f/5f411f08-45dd-4d73-8894-daf51c175150.jpg?1601064619";
        else if(id.equals("491688"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/a/5adcb500-8c77-4925-8e2c-1243502827d1.jpg?1601065203";
        else if(id.equals("491689"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/a/5adcb500-8c77-4925-8e2c-1243502827d1.jpg?1601065203";
        else if(id.equals("491693"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/0/301750a7-d1fd-435e-bfa8-9d2fb22ad627.jpg?1601065373";
        else if(id.equals("491694"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/0/301750a7-d1fd-435e-bfa8-9d2fb22ad627.jpg?1601065373";
        else if(id.equals("491706"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/9/193071fe-180b-4d35-ba78-9c16675c29fc.jpg?1601065398";
        else if(id.equals("491707"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/9/193071fe-180b-4d35-ba78-9c16675c29fc.jpg?1601065398";
        else if(id.equals("491711"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/11568cdf-6148-494c-8b98-f5ca5797d775.jpg?1601065596";
        else if(id.equals("491712"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/1/11568cdf-6148-494c-8b98-f5ca5797d775.jpg?1601065596";
        else if(id.equals("491718"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/9/890eee8d-a339-4143-adfa-1b17ec10c099.jpg?1601065664";
        else if(id.equals("491719"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/9/890eee8d-a339-4143-adfa-1b17ec10c099.jpg?1601065664";
        else if(id.equals("491723"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/7/67f4c93b-080c-4196-b095-6a120a221988.jpg?1601065680";
        else if(id.equals("491724"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/7/67f4c93b-080c-4196-b095-6a120a221988.jpg?1601065680";
        else if(id.equals("491725"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/2/32779721-b021-4bd4-95d1-4a19b78d9faa.jpg?1601065728";
        else if(id.equals("491726"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/2/32779721-b021-4bd4-95d1-4a19b78d9faa.jpg?1601065728";
        else if(id.equals("491741"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/c/7c04c734-354d-4925-8161-7052110951df.jpg?1601065747";
        else if(id.equals("491742"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/c/7c04c734-354d-4925-8161-7052110951df.jpg?1601065747";
        else if(id.equals("491747"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/0/609d3ecf-f88d-4268-a8d3-4bf2bcf5df60.jpg?1601065770";
        else if(id.equals("491748"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/0/609d3ecf-f88d-4268-a8d3-4bf2bcf5df60.jpg?1601065770";
        else if(id.equals("491757"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/6/e63f8b20-f45b-4293-9aac-cdc021939be6.jpg?1601065790";
        else if(id.equals("491758"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/6/e63f8b20-f45b-4293-9aac-cdc021939be6.jpg?1601065790";
        else if(id.equals("491770"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/8/98496d5b-1519-4f0c-8b46-0a43be643dfb.jpg?1601065841";
        else if(id.equals("491771"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/8/98496d5b-1519-4f0c-8b46-0a43be643dfb.jpg?1601065841";
        else if(id.equals("491773"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/8/d8ed0335-daa6-4dbe-a94d-4d56c8cfd093.jpg?1601065859";
        else if(id.equals("491774"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/8/d8ed0335-daa6-4dbe-a94d-4d56c8cfd093.jpg?1601065859";
        else if(id.equals("491786"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/5/75240bbc-adc7-48ff-9523-c79776d710d3.jpg?1601065917";
        else if(id.equals("491787"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/5/75240bbc-adc7-48ff-9523-c79776d710d3.jpg?1601065917";
        else if(id.equals("491802"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/c/bc7239ea-f8aa-4a6f-87bd-c35359635673.jpg?1601065932";
        else if(id.equals("491803"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/c/bc7239ea-f8aa-4a6f-87bd-c35359635673.jpg?1601065932";
        else if(id.equals("491807"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/8/782ca27f-9f18-476c-b582-89c06fb2e322.jpg?1601065955";
        else if(id.equals("491808"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/8/782ca27f-9f18-476c-b582-89c06fb2e322.jpg?1601065955";
        else if(id.equals("491809"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/6/a69541db-3f4e-412f-aa8e-dec1e74f74dc.jpg?1601066019";
        else if(id.equals("491810"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/6/a69541db-3f4e-412f-aa8e-dec1e74f74dc.jpg?1601066019";
        else if(id.equals("491818"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/2/228e551e-023a-4c9a-8f32-58dae6ffdf7f.jpg?1601066052";
        else if(id.equals("491819"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/2/228e551e-023a-4c9a-8f32-58dae6ffdf7f.jpg?1601066052";
        else if(id.equals("491825"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/5/c5cb3052-358d-44a7-8cfd-cd31b236494a.jpg?1601066068";
        else if(id.equals("491826"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/5/c5cb3052-358d-44a7-8cfd-cd31b236494a.jpg?1601066068";
        else if(id.equals("491835"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/f/2f632537-63bf-4490-86e6-e6067b9c1a3b.jpg?1601066084";
        else if(id.equals("491836"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/f/2f632537-63bf-4490-86e6-e6067b9c1a3b.jpg?1601066084";
        else if(id.equals("491839"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/9/99535539-aa73-41ed-86ab-21c97b92620d.jpg?1601066100";
        else if(id.equals("491840"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/9/99535539-aa73-41ed-86ab-21c97b92620d.jpg?1601066100";
        else if(id.equals("491859"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/3/235d1ffc-72aa-40a2-95dc-3f6a8d495061.jpg?1601066116";
        else if(id.equals("491860"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/3/235d1ffc-72aa-40a2-95dc-3f6a8d495061.jpg?1601066116";
        else if(id.equals("491864"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/1/61bd69ea-1e9e-46b0-b1a1-ed7fdbe3deb6.jpg?1601066131";
        else if(id.equals("491865"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/1/61bd69ea-1e9e-46b0-b1a1-ed7fdbe3deb6.jpg?1601066131";
        else if(id.equals("491866"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/a/3a7fd24e-84d8-405d-86e4-0571a9e23cc2.jpg?1601066149";
        else if(id.equals("491867"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/a/3a7fd24e-84d8-405d-86e4-0571a9e23cc2.jpg?1601066149";
        else if(id.equals("491909"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/5/0511e232-2a72-40f5-a400-4f7ebc442d17.jpg?1601066169";
        else if(id.equals("491910"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/5/0511e232-2a72-40f5-a400-4f7ebc442d17.jpg?1601066169";
        else if(id.equals("491911"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/2/d24c3d51-795d-4c01-a34a-3280fccd2d78.jpg?1601066301";
        else if(id.equals("491912"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/2/d24c3d51-795d-4c01-a34a-3280fccd2d78.jpg?1601066301";
        else if(id.equals("491913"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/4/b4b99ebb-0d54-4fe5-a495-979aaa564aa8.jpg?1601066318";
        else if(id.equals("491914"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/4/b4b99ebb-0d54-4fe5-a495-979aaa564aa8.jpg?1601066318";
        else if(id.equals("491915"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/a/da57eb54-5199-4a56-95f7-f6ac432876b1.jpg?1601066338";
        else if(id.equals("491916"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/a/da57eb54-5199-4a56-95f7-f6ac432876b1.jpg?1601066338";
        else if(id.equals("491918"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/5/6559047e-6ede-4815-a3a0-389062094f9d.jpg?1601066358";
        else if(id.equals("491919"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/5/6559047e-6ede-4815-a3a0-389062094f9d.jpg?1601066358";
        else if(id.equals("491920"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/6/2668ac91-6cda-4f81-a08d-4fc5f9cb35b2.jpg?1601066375";
        else if(id.equals("491921"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/6/2668ac91-6cda-4f81-a08d-4fc5f9cb35b2.jpg?1601066375";
        else if(id.equals("495098"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/e/ae92e656-6c9d-48c3-a238-5a11c2c62ec8.jpg?1599831846";
        else if(id.equals("495099"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/8/589a324f-4466-4d4a-8cfb-806a041d7c1f.jpg?1599831830";
        else if(id.equals("495100"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/9/1967d4a8-6cc4-4a4d-9d24-93257de35e6d.jpg?1599831973";
        else if(id.equals("495101"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/c/3c6a38dd-e8f5-420f-9576-66937c71286b.jpg?1599832044";
        else if(id.equals("495102"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/b/2b90e88b-60a3-4d1d-bb8c-14633e5005a5.jpg?1599832083";
        else if(id.equals("29530711"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/9/4912a0a5-2fec-4c6b-9545-9ab0c4e25268.jpg?1599764491";
        else if(id.equals("1750411"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/f/8f047a8b-6c94-4b99-bcaa-10680400ee25.jpg?1562073449";
        else if(id.equals("5176911"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/b/cbbd8a12-d916-4fb1-994a-7d4a3e2ae2ab.jpg?1562935938";
        else if(id.equals("44680711"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/0/a00a7180-49bd-4ead-852a-67b6b5e4b933.jpg?1564694995";
        else if(id.equals("295726t") || id.equals("295673t") || id.equals("295532t")) //Servo 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/7/d79e2bf1-d26d-4be3-a5ad-a43346ed445a.jpg?1562640071";
        else if(id.equals("295632t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/e/1ebc91a9-23e0-4ca1-bc6d-e710ad2efb31.jpg?1561756762";
        else if(id.equals("295802"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/c/4cb8d03e-e1d2-451e-97a8-141082f92501.jpg?1598627140";
        else if(id.equals("497724t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/6/661cbde4-9444-4259-b2cf-7c8f9814a071.jpg?1604666353";
        else if(id.equals("295810t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/458f44dd-83f1-497e-b5d0-e3417eb9dfec.jpg?1592672533";
        else if(id.equals("476226"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/a/caa7922e-3313-4f12-b91e-95aaa2d76cc2.jpg?1574133191";
        else if(id.equals("476217"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/9/a9a6cf9c-3560-435c-b0ec-8653a9dc7776.jpg?1578369133";
         else if(id.equals("503619"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/7/97502411-5c93-434c-b77b-ceb2c32feae7.jpg?1608253263";
         else if(id.equals("503620"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/7/97502411-5c93-434c-b77b-ceb2c32feae7.jpg?1608253263";
         else if(id.equals("503626"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/6/3606519e-5677-4c21-a34e-be195b6669fa.jpg?1611000242";
         else if(id.equals("503627"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/6/3606519e-5677-4c21-a34e-be195b6669fa.jpg?1611000242";
         else if(id.equals("503646"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/d/5d131784-c1a3-463e-a37b-b720af67ab62.jpg?1611323243";
         else if(id.equals("503647"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/d/5d131784-c1a3-463e-a37b-b720af67ab62.jpg?1611323243";
         else if(id.equals("503657"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/a/fab2fca4-a99f-4ffe-9c02-edb6e0be2358.jpg?1611862762";
         else if(id.equals("503658"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/a/fab2fca4-a99f-4ffe-9c02-edb6e0be2358.jpg?1611862762";
         else if(id.equals("503700"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/d/9dfdb73d-b001-4a59-b79e-8c8c1baea116.jpg?1610397419";
         else if(id.equals("503701"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/d/9dfdb73d-b001-4a59-b79e-8c8c1baea116.jpg?1610397419";
         else if(id.equals("503721"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/4/14dc88ee-bba9-4625-af0d-89f3762a0ead.jpg?1610586381";
         else if(id.equals("503722"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/4/14dc88ee-bba9-4625-af0d-89f3762a0ead.jpg?1610586381";
         else if(id.equals("503724"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1610153366";
         else if(id.equals("503725"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1610153366";
         else if(id.equals("503734"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/4/44657ab1-0a6a-4a5f-9688-86f239083821.jpg?1611054410";
         else if(id.equals("503735"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/4/44657ab1-0a6a-4a5f-9688-86f239083821.jpg?1611054410";
         else if(id.equals("503766"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/2/22a6a5f1-1405-4efb-af3e-e1f58d664e99.jpg?1610652603";
         else if(id.equals("503767"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/2/22a6a5f1-1405-4efb-af3e-e1f58d664e99.jpg?1610652603";
         else if(id.equals("503781"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/6/f6cd7465-9dd0-473c-ac5e-dd9e2f22f5f6.jpg?1610295185";
         else if(id.equals("503782"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/6/f6cd7465-9dd0-473c-ac5e-dd9e2f22f5f6.jpg?1610295185";
         else if(id.equals("503793"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/6/c697548f-925b-405e-970a-4e78067d5c8e.jpg?1610996900";
         else if(id.equals("503794"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/6/c697548f-925b-405e-970a-4e78067d5c8e.jpg?1610996900";
         else if(id.equals("503796"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/7/b76bed98-30b1-4572-b36c-684ada06826c.jpg?1610368590";
         else if(id.equals("503797"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/7/b76bed98-30b1-4572-b36c-684ada06826c.jpg?1610368590";
         else if(id.equals("503867"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b6de14ae-0132-4261-af00-630bf15918cd.jpg?1608226844";
         else if(id.equals("503868"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/6/b6de14ae-0132-4261-af00-630bf15918cd.jpg?1608226844";
         else if(id.equals("503869"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/c/0ce39a19-f51d-4a35-ae80-5b82eb15fcff.jpg?1608058149";
         else if(id.equals("503870"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/c/0ce39a19-f51d-4a35-ae80-5b82eb15fcff.jpg?1608058149";
         else if(id.equals("503872"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/7/87a4e5fe-161f-42da-9ca2-67c8e8970e94.jpg?1608057969";
         else if(id.equals("503873"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/7/87a4e5fe-161f-42da-9ca2-67c8e8970e94.jpg?1608057969";
         else if(id.equals("503879"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/e/7ef37cb3-d803-47d7-8a01-9c803aa2eadc.jpg?1608058266";
         else if(id.equals("503880"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/e/7ef37cb3-d803-47d7-8a01-9c803aa2eadc.jpg?1608058266";
         else if(id.equals("503837t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/4/54a1c6a9-3531-4432-9157-e4400dbc89fd.jpg?1611206522";
         else if(id.equals("503841t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/f/df826c7d-5508-4e21-848c-91bc3e3f447a.jpg?1611206331";
         else if(id.equals("473148"))
            cardurl = "https://img.scryfall.com/cards/large/front/5/d/5dca90ef-1c17-4dcc-9fef-dab9ee92f590.jpg?1572490726";
         else if(id.equals("473127t"))
            cardurl = "https://img.scryfall.com/cards/large/front/9/4/94057dc6-e589-4a29-9bda-90f5bece96c4.jpg?1572489125";
         else if(id.equals("295910t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/b/7b993828-e139-4cb6-a329-487accc1c515.jpg?1563073064";
        else if(id.equals("296315t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/d/ed58cd8c-b11a-4109-b789-0eb92eaf0184.jpg?1614969127";
        else if(id.equals("296247t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/7/07027a7c-5843-4d78-9b86-8799363c0b82.jpg?1591319174";
        else if(id.equals("296217t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/7/e72daa68-0680-431c-a616-b3693fd58813.jpg?1614969153";
        else if(id.equals("296145t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/b/cbcb0668-e88c-4462-b079-34f140c0277e.jpg?1614969101";
        else if(id.equals("295986t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/a/4a2144f2-d4be-419e-bfca-116cedfdf18b.jpg?1614968857";
        else if(id.equals("518429t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/6/f62080da-a11b-4da3-bb8f-57f543bf076a.jpg?1618767682";
        else if(id.equals("513482"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/8/18a2bdc8-b705-4eb5-b3a5-ff2e2ab8f312.jpg?1617901976";
        else if(id.equals("513483"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/8/18a2bdc8-b705-4eb5-b3a5-ff2e2ab8f312.jpg?1617901976";
        else if(id.equals("513624"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/9/d9131fc3-018a-4975-8795-47be3956160d.jpg?1617452954";
        else if(id.equals("513625"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/9/d9131fc3-018a-4975-8795-47be3956160d.jpg?1617452954";
        else if(id.equals("513626"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/2/c204b7ca-0904-40fa-b20c-92400fae20b8.jpg?1617583842";
        else if(id.equals("513627"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/2/c204b7ca-0904-40fa-b20c-92400fae20b8.jpg?1617583842";
        else if(id.equals("513628"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/a/ba09360a-067e-48a5-bdc5-a19fd066a785.jpg?1617453022";
        else if(id.equals("513629"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/a/ba09360a-067e-48a5-bdc5-a19fd066a785.jpg?1617453022";
        else if(id.equals("513630"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/d/0dba25e3-2b4f-45d4-965f-3834bcb359ee.jpg?1617453074";
        else if(id.equals("513631"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/d/0dba25e3-2b4f-45d4-965f-3834bcb359ee.jpg?1617453074";
        else if(id.equals("513632"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/7/d7148d24-373e-4485-860b-c3429c2337f2.jpg?1618163722";
        else if(id.equals("513633"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/7/d7148d24-373e-4485-860b-c3429c2337f2.jpg?1618163722";
        else if(id.equals("513634"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/b/8b45dc40-6827-46a7-a9b7-802be698d053.jpg?1617453130";
        else if(id.equals("513635"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/b/8b45dc40-6827-46a7-a9b7-802be698d053.jpg?1617453130";
        else if(id.equals("513636"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/e/8e4e0f81-f92b-4a3a-bb29-adcc3de211b4.jpg?1617363020";
        else if(id.equals("513637"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/e/8e4e0f81-f92b-4a3a-bb29-adcc3de211b4.jpg?1617363020";
        else if(id.equals("513638"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/a/aaa1e6be-08cc-4ccc-b2de-3511613e4fd0.jpg?1617612926";
        else if(id.equals("513639"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/a/aaa1e6be-08cc-4ccc-b2de-3511613e4fd0.jpg?1617612926";
        else if(id.equals("513640"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/b/5bd9b5cf-f018-48af-a081-995ce8ecc539.jpg?1617453238";
        else if(id.equals("513641"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/b/5bd9b5cf-f018-48af-a081-995ce8ecc539.jpg?1617453238";
        else if(id.equals("513642"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/8/18c16872-3675-4a4d-962a-2e17ad6f3886.jpg?1618326108";
        else if(id.equals("513643"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/8/18c16872-3675-4a4d-962a-2e17ad6f3886.jpg?1618326108";
        else if(id.equals("513644"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/9/8982ff88-8595-4363-8cde-6e87fb57a2d8.jpg?1617400731";
        else if(id.equals("513645"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/9/8982ff88-8595-4363-8cde-6e87fb57a2d8.jpg?1617400731";
        else if(id.equals("513646"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/3/938cee8f-ac2c-49a5-9ff7-1367d0edfabe.jpg?1617453576";
        else if(id.equals("513647"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/3/938cee8f-ac2c-49a5-9ff7-1367d0edfabe.jpg?1617453576";
        else if(id.equals("513648"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/7/87463b68-3642-41c7-a11c-67d524759b60.jpg?1617453416";
        else if(id.equals("513649"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/7/87463b68-3642-41c7-a11c-67d524759b60.jpg?1617453416";
        else if(id.equals("513650"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/c/8cfd0887-0c83-4b33-a85e-8b8ec5bf758d.jpg?1617453475";
        else if(id.equals("513651"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/c/8cfd0887-0c83-4b33-a85e-8b8ec5bf758d.jpg?1617453475";
        else if(id.equals("513652"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/5/65008352-bc7e-40b2-a832-b46813e5dc4c.jpg?1617453527";
        else if(id.equals("513653"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/5/65008352-bc7e-40b2-a832-b46813e5dc4c.jpg?1617453527";
        else if(id.equals("513652t") || id.equals("513638t") || id.equals("513543t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/0/d0ddbe3e-4a66-494d-9304-7471232549bf.jpg?1617626099";
        else if(id.equals("513634t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/1/910f48ab-b04e-4874-b31d-a86a7bc5af14.jpg?1617626097";
        else if(id.equals("296380t")) // Construct */*
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/5/c5eafa38-5333-4ef2-9661-08074c580a32.jpg?1562702317";
        else if(id.equals("530447"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/f/6f509dbe-6ec7-4438-ab36-e20be46c9922.jpg?1626139695";
        else if(id.equals("530448"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/9/59b11ff8-f118-4978-87dd-509dc0c8c932.jpg?1626297774";
        else if(id.equals("530449"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/0/70b284bd-7a8f-4b60-8238-f746bdc5b236.jpg?1626297777";
        else if(id.equals("530448t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1626139812";
        else if(id.equals("530447t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/a/fa6fdb57-82f3-4695-b1fa-1f301ea4ef83.jpg?1626139846";
        else if(id.equals("527514t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/4/c49e8e79-8673-41c2-a1ad-273c37e27aca.jpg?1625767076";
        else if(id.equals("527507t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/b/6b2c8f52-1580-42d5-8434-c4c70e31e31b.jpg?1626139372";
        else if(id.equals("527307t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/9/a9c981c9-3376-4f6e-b30d-859e5fc7347e.jpg?1626138970";
        else if(id.equals("96946t") || id.equals("338397t"))
            cardurl = "https://static.cardmarket.com/img/065612b0892a18c27f4de6a50c5d0327/items/1/GK1/366030.jpg";
        else if(id.equals("439314"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/4/24842e29-77ac-4904-bd8f-b2cd163dd357.jpg?1562701950";
        else if(id.equals("439315"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/4/24842e29-77ac-4904-bd8f-b2cd163dd357.jpg?1562701950";
        else if(id.equals("439316"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/7/d78cd000-3908-446d-b155-dd8af3d8f166.jpg?1562702374";
        else if(id.equals("439317"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/7/d78cd000-3908-446d-b155-dd8af3d8f166.jpg?1562702374";
        else if(id.equals("439318"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/e/ce0a6fa9-f664-4263-8deb-8112f860814c.jpg?1562702348";
        else if(id.equals("439318t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/462ff49b-a004-4dab-a25b-65cb18c1bbec.jpg?1592672584";
        else if(id.equals("439319"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/e/ce0a6fa9-f664-4263-8deb-8112f860814c.jpg?1562702348";
        else if(id.equals("439320"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/2/3216d161-a43d-4a55-a14b-098061805409.jpg?1562702004";
        else if(id.equals("439320t"))
            cardurl = "https://static.cardmarket.com/img/a8462480806adfd76fb002d92e976d96/items/1/UST/313929.jpg";
        else if(id.equals("439321"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/2/3216d161-a43d-4a55-a14b-098061805409.jpg?1562702004";
        else if(id.equals("439321t"))
            cardurl = "https://static.cardmarket.com/img/a8462480806adfd76fb002d92e976d96/items/1/UST/313929.jpg";
        else if(id.equals("439322"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/b/2b45fb19-450d-40bd-91c7-b5ace4a77f2a.jpg?1625771739";
        else if(id.equals("439323"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/b/eba8bb03-6093-4e2b-99a2-a3fc5d8eb659.jpg?1625771729";
        else if(id.equals("439324"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/e/7e0cfe44-9b57-4b9a-b23f-18d3237bd7ee.jpg?1562702153";
        else if(id.equals("439325"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/e/7e0cfe44-9b57-4b9a-b23f-18d3237bd7ee.jpg?1562702153";
        else if(id.equals("439326"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/8/28059d09-2c7d-4c61-af55-8942107a7c1f.jpg?1562701962";
        else if(id.equals("439327"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/8/28059d09-2c7d-4c61-af55-8942107a7c1f.jpg?1562701962";
        else if(id.equals("439328"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/6/a66d5ee9-86a7-4052-a868-8dc6398342b3.jpg?1562702238";
        else if(id.equals("439329"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/6/a66d5ee9-86a7-4052-a868-8dc6398342b3.jpg?1562702238";
        else if(id.equals("439330"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/b/bb89599a-1883-45da-a87a-25e3f70c5a33.jpg?1562702297";
        else if(id.equals("439330t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/462ff49b-a004-4dab-a25b-65cb18c1bbec.jpg?1592672584";
        else if(id.equals("439331"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/b/bb89599a-1883-45da-a87a-25e3f70c5a33.jpg?1562702297";
        else if(id.equals("439331t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/a/7a49607c-427a-474c-ad77-60cd05844b3c.jpg?1562639871";
        else if(id.equals("439332"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/3/b3c2bd44-4d75-4f61-89c0-1f1ba4d59ffa.jpg?1625771753";
        else if(id.equals("439333"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/d/4d553078-afaf-42db-879b-fb4cb4d25742.jpg?1562702046";
        else if(id.equals("439333t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/462ff49b-a004-4dab-a25b-65cb18c1bbec.jpg?1592672584";
        else if(id.equals("439334"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/d/4d553078-afaf-42db-879b-fb4cb4d25742.jpg?1562702046";
        else if(id.equals("439335"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/c/cc750c64-fd83-4b7b-9a40-a99213e6fa6d.jpg?1562702327";
        else if(id.equals("439336"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/c/cc750c64-fd83-4b7b-9a40-a99213e6fa6d.jpg?1562702327";
        else if(id.equals("439337"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/4/6473e356-2685-4f91-ab42-cca8c6be0816.jpg?1562702104";
        else if(id.equals("439338"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/4/6473e356-2685-4f91-ab42-cca8c6be0816.jpg?1562702104";
        else if(id.equals("439339"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/1/b101dd14-aff1-4811-bf1b-468930dd2999.jpg?1562702260";
        else if(id.equals("439339t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1562844819";
        else if(id.equals("439340"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/1/b101dd14-aff1-4811-bf1b-468930dd2999.jpg?1562702260";
        else if(id.equals("439341"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/3/63b2b7cd-a51d-4e50-b794-a52731196973.jpg?1562702090";
        else if(id.equals("439342"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/3/63b2b7cd-a51d-4e50-b794-a52731196973.jpg?1562702090";
        else if(id.equals("530449t"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/5/65f8e40f-fb5e-4ab8-add3-a8b87e7bcdd9.jpg?1626139916";
        else if(id.equals("435173") || id.equals("435174") || id.equals("435176t") || id.equals("435212t") || id.equals("435227") || id.equals("435244") 
                || id.equals("435328") || id.equals("435347") || id.equals("435388t") || id.equals("435391") || id.equals("435392t") || id.equals("435393t") 
                || id.equals("435402") || id.equals("435409") || id.equals("435410t") || id.equals("435411t") || id.equals("435173t") || id.equals("435174t") 
                || id.equals("435181t") || id.equals("435226") || id.equals("435243") || id.equals("435327") || id.equals("435346") || id.equals("435380t")
                || id.equals("435390") || id.equals("435392") || id.equals("435393") || id.equals("435401") || id.equals("435408") || id.equals("435410") 
                || id.equals("435411"))
            cardurl = "http://teksport.altervista.org/XLN/" + id + ".jpg";
        else if(id.equals("409741") || id.equals("409760") || id.equals("409790") || id.equals("409826t") || id.equals("409839") || id.equals("409856") 
                || id.equals("409868") || id.equals("409901") || id.equals("409913") || id.equals("409946") || id.equals("409962") || id.equals("409976") 
                || id.equals("409993") || id.equals("410015t") || id.equals("410027") || id.equals("410049") || id.equals("409742") || id.equals("409773") 
                || id.equals("409791") || id.equals("409831") || id.equals("409840") || id.equals("409860t") || id.equals("409869") || id.equals("409903t") 
                || id.equals("409923") || id.equals("409947") || id.equals("409968") || id.equals("409977") || id.equals("410007") || id.equals("410016t") 
                || id.equals("410031t") || id.equals("410049t") || id.equals("409743") || id.equals("409774") || id.equals("409796") || id.equals("409832") 
                || id.equals("409843") || id.equals("409862t") || id.equals("409897") || id.equals("409910") || id.equals("409924") || id.equals("409951") 
                || id.equals("409969") || id.equals("409987") || id.equals("410007t") || id.equals("410021") || id.equals("410032t") || id.equals("410050") 
                || id.equals("409744") || id.equals("409786") || id.equals("409797") || id.equals("409836") || id.equals("409844") || id.equals("409864") 
                || id.equals("409898") || id.equals("409911") || id.equals("409937") || id.equals("409952") || id.equals("409970") || id.equals("409988") 
                || id.equals("410008") || id.equals("410022") || id.equals("410033") || id.equals("410050t") || id.equals("409759") || id.equals("409787") 
                || id.equals("409805t") || id.equals("409837") || id.equals("409855") || id.equals("409865") || id.equals("409900") || id.equals("409912") 
                || id.equals("409938") || id.equals("409961") || id.equals("409971") || id.equals("409992") || id.equals("410008t") || id.equals("410026") 
                || id.equals("410034"))
            cardurl = "http://teksport.altervista.org/SOI/" + id + ".jpg";
        else if(id.equals("439401") || id.equals("439471") || id.equals("439625") || id.equals("439628") || id.equals("439631") || id.equals("439633") 
                || id.equals("439636") || id.equals("439639") || id.equals("439642") || id.equals("439644t") || id.equals("439646t") || id.equals("439649") 
                || id.equals("439651") || id.equals("439654") || id.equals("439438") || id.equals("439502") || id.equals("439626") || id.equals("439629") 
                || id.equals("439631t") || id.equals("439634") || id.equals("439637") || id.equals("439640") || id.equals("439643") || id.equals("439645") 
                || id.equals("439647") || id.equals("439649t") || id.equals("439652") || id.equals("439456") || id.equals("439536") || id.equals("439627") 
                || id.equals("439630") || id.equals("439632") || id.equals("439635") || id.equals("439638") || id.equals("439641") || id.equals("439644") 
                || id.equals("439646") || id.equals("439648") || id.equals("439650") || id.equals("439653"))
            cardurl = "http://teksport.altervista.org/UST/" + id + ".jpg";
        else if(set.equals("S00"))
            cardurl = "http://teksport.altervista.org/S00/" + id + ".jpg";
        else if (id.equals("495186"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/0/c0250dc8-9d4c-428a-9e34-9e3577be4745.jpg?1604268817";
        else if (id.equals("495187"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/8/48b8024d-a300-43cb-9dde-6b4cb1fa19f7.jpg?1604202315";
        else if (id.equals("495188"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/4/d442c32d-457d-4fef-bba2-33a07bf23125.jpg?1604202355";
        else if (id.equals("495189"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/e/be97b691-f9f5-4fb4-8e44-8ffe32d13d03.jpg?1604202388";
        else if (id.equals("495190"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/8/e8df0aed-dd2b-4f1e-8dfe-aec07462b1e1.jpg?1604202426";
        else if (id.equals("495191"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/f/efc72e9f-2cda-47b9-84fd-4eed88312404.jpg?1604202443";
        else if (id.equals("495192"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/4/04833fcc-cef7-4152-8191-c552288c83e4.jpg?1604202462";
        else if (id.equals("495193"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/6/d60c9b15-c824-4203-bdda-ff9c041f9e2f.jpg?1604202489";
        else if (id.equals("495194"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/5/05347539-de61-4a37-929f-c909e65033f5.jpg?1604202520";
        else if (id.equals("495195"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b5757230-08b8-4808-af61-d343f9748fb1.jpg?1604202554";
        else if (id.equals("495196"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/e/aecfbd48-7da0-4b44-b9a2-d31412f65eb1.jpg?1604202574";
        else if (id.equals("495197"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/9/490f3d74-6144-4cbc-80ed-37cfcdbd159a.jpg?1604202592";
        else if (id.equals("495198"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b6008794-a7ca-4a3e-b88b-e5dbb9e0f39b.jpg?1604202633";
        else if (id.equals("495199"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/9/6954cc66-ab80-4457-b0da-61d80e80e25e.jpg?1604202679";
        else if (id.equals("495200"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/5/d52e90d3-d356-4b23-8f5c-a4004b20394c.jpg?1604202724";
        else if (id.equals("495201"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/9/09c8c150-a0d8-4254-9169-7697e9c540da.jpg?1604202798";
        else if (id.equals("495202"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/9/09c8c150-a0d8-4254-9169-7697e9c540da.jpg?1604202798";
        else if (id.equals("495203"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/9/796b5899-97e5-4682-aac8-51378f0c904e.jpg?1604202817";
        else if (id.equals("495204"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/5/151bdf3a-4445-43b1-8cea-2737c13d9dee.jpg?1604202856";
        else if (id.equals("495205"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/0/309b2cb5-b9a8-417d-b5ae-0a7d03ff93f0.jpg?1604202878"; 
        else if (id.equals("495206"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/3/03d6d8a4-c51d-4b4a-86e7-df9e9c7a171d.jpg?1604202897";
        else if (id.equals("495207"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/f/cf4a4aba-3391-4259-9a5f-a163a45d943c.jpg?1604202922";
        else if (id.equals("495208"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/d/0d0954df-07f0-430d-90ee-d1fe40af546f.jpg?1604202961";
        else if (id.equals("495209"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/b/9b5bc5d7-c0f8-4632-adb7-dd3b75a3d87d.jpg?1607363660";
        else if (id.equals("495210"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/3/c344a3cd-43e0-4333-83ec-081f0e39530a.jpg?1604203001";
        else if (id.equals("495210t")) //Plant 0/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/0/d03d87f5-0ac6-45ca-a54b-6a36132a8eae.jpg?1604194870";
        else if (id.equals("495205t")) //Insect 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/4/84da9c36-5d9c-4e29-b6cc-c5c10e490f2e.jpg?1604194822";
        else if (id.equals("495188t")) //Cat Beast 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/2/e2c91781-acf9-4cff-be1a-85148ad2a683.jpg?1604194683";
        else if (id.equals("425847"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/e/2e376bdf-076c-471a-9408-b36fc5b8405b.jpg?1593812924";
        else if (id.equals("293395") || id.equals("29339510"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/4/940509ec-8f58-4593-a598-142a827f55b0.jpg?1573507789";
        else if (id.equals("17498") || id.equals("1749810"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/c/4c663245-dfb6-4d92-8ac7-ffe3d5d12187.jpg?1562066227";
        else if (id.equals("51974") || id.equals("5197410"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/c/ccdda4dd-f2e3-419e-9f4d-15d7270e27ee.jpg?1562548183";
        else if (id.equals("52495"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/c/5cf835fb-4953-486c-aed2-2208ca31df31.jpg?1562545025";
        else if (id.equals("5249510"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/c/5cf835fb-4953-486c-aed2-2208ca31df31.jpg?1562545025";
         else if (id.equals("52473"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/7/870e6492-3e4d-4680-9a78-a99782039876.jpg?1562546187";
        else if (id.equals("5247310"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/7/870e6492-3e4d-4680-9a78-a99782039876.jpg?1562546187";
        else if (id.equals("52137"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/466f7f14-72b7-46c9-b8d6-a99bf92c4089.jpg?1562544411";
        else if (id.equals("5213710"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/6/466f7f14-72b7-46c9-b8d6-a99bf92c4089.jpg?1562544411";
        else if (id.equals("52530"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/f/bfc92a35-9e40-4a7b-a7cb-f0b4537ea996.jpg?1562547835";
        else if (id.equals("5253010"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/f/bfc92a35-9e40-4a7b-a7cb-f0b4537ea996.jpg?1562547835";
        else if (id.equals("52704"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/2/72887f7f-4156-4b88-aef5-b96dea57903e.jpg?1562545665";
        else if (id.equals("5270410"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/2/72887f7f-4156-4b88-aef5-b96dea57903e.jpg?1562545665"; 
        else if(id.equals("296818"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/3/6317573e-d892-48ce-bba4-76f9f632ed2b.jpg?1630243608";
        else if(id.equals("296817"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/e/7e6b3fb3-897b-4665-b053-a29f25850b25.jpg?1630243654";
        else if(id.equals("296594"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/3/6317573e-d892-48ce-bba4-76f9f632ed2b.jpg?1630243608";
        else if(id.equals("296486"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/e/7e6b3fb3-897b-4665-b053-a29f25850b25.jpg?1630243654";
        else if(id.equals("296764t") || id.equals("534957t") || id.equals("535010t") || id.equals("534872t") || id.equals("534839t") 
                || id.equals("534774t") || id.equals("540708t")) // Clue
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/2/f2c859e1-181e-44d1-afbd-bbd6e52cf42a.jpg?1562086885";
        else if(id.equals("296695t")) //Squirrel 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/7/977ddd05-1aae-46fc-95ce-866710d1c5c6.jpg?1626092815";
        else if(id.equals("296549t")) // Djinn Monk 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/2/f2e8077e-4400-4923-afe6-6ff5a51b5e91.jpg?1561758421";
        else if(id.equals("296439t")) //Kraken 8/8
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/a/ca17c7b2-180a-4bd1-9ab2-152f8f656dba.jpg?1591225580";
        else if(id.equals("999993")) // Day
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/c/9c0f7843-4cbb-4d0f-8887-ec823a9238da.jpg?1630606483";
        else if(id.equals("999994")) // Night
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/c/9c0f7843-4cbb-4d0f-8887-ec823a9238da.jpg?1630606483";
        else if(id.equals("534752"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/4/54d4e7c3-294d-4900-8b70-faafda17cc33.jpg?1631313886";
        else if(id.equals("534753"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/4/54d4e7c3-294d-4900-8b70-faafda17cc33.jpg?1631313886";
        else if(id.equals("534754"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/1/6109b54e-56c5-4014-9f6d-d5f7a0fd725d.jpg?1630610048";
        else if(id.equals("534755"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/1/6109b54e-56c5-4014-9f6d-d5f7a0fd725d.jpg?1630610048";
        else if(id.equals("534756"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/a/4adee830-62fd-4ab4-b1c6-a8bbe15331d1.jpg?1631314326";
        else if(id.equals("534757"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/a/4adee830-62fd-4ab4-b1c6-a8bbe15331d1.jpg?1631314326";
        else if(id.equals("534760"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/d/0dbac7ce-a6fa-466e-b6ba-173cf2dec98e.jpg?1630658675";
        else if(id.equals("534761"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/d/0dbac7ce-a6fa-466e-b6ba-173cf2dec98e.jpg?1630658675";
        else if(id.equals("534767"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/0/20e94e17-2e4c-41cd-8cc5-39ab41037287.jpg?1630994373";
        else if(id.equals("534768"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/0/20e94e17-2e4c-41cd-8cc5-39ab41037287.jpg?1630994373";
        else if(id.equals("534772"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/2/a204c2a3-a899-4b70-8825-7e085b655ed0.jpg?1630562155";
        else if(id.equals("534773"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/2/a204c2a3-a899-4b70-8825-7e085b655ed0.jpg?1630562155";
        else if(id.equals("534783"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/2/d2704743-2e23-40b9-a367-c73d2db45afc.jpg?1631299609";
        else if(id.equals("534784"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/2/d2704743-2e23-40b9-a367-c73d2db45afc.jpg?1631299609";
        else if(id.equals("534785"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/d/2d3687e2-09e0-4753-aa02-88a19bde3330.jpg?1631299761";
        else if(id.equals("534786"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/d/2d3687e2-09e0-4753-aa02-88a19bde3330.jpg?1631299761";
        else if(id.equals("534800"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/6/36e71d16-0964-489d-bea2-9cec7991fc99.jpg?1630606806";
        else if(id.equals("534801"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/6/36e71d16-0964-489d-bea2-9cec7991fc99.jpg?1630606806";
        else if(id.equals("534804"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/3/03a3ea4b-d292-4602-985f-7a7971ca73ec.jpg?1631299811";
        else if(id.equals("534805"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/3/03a3ea4b-d292-4602-985f-7a7971ca73ec.jpg?1631299811";
        else if(id.equals("534807"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/b/abff6c81-65a4-48fa-ba8f-580f87b0344a.jpg?1631043988";
        else if(id.equals("534808"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/b/abff6c81-65a4-48fa-ba8f-580f87b0344a.jpg?1631043988";
        else if(id.equals("534816"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/b/eb34c472-c6ff-4d83-ac8b-a8f279593f98.jpg?1631304637";
        else if(id.equals("534817"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/b/eb34c472-c6ff-4d83-ac8b-a8f279593f98.jpg?1631304637";
        else if(id.equals("534823"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/7/e79269af-63eb-43d2-afee-c38fa14a0c5b.jpg?1631225854";
        else if(id.equals("534824"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/7/e79269af-63eb-43d2-afee-c38fa14a0c5b.jpg?1631225854";
        else if(id.equals("534826"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/a/caa57b63-bb11-45e8-8795-de92ca61f4f1.jpg?1631303677";
        else if(id.equals("534827"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/c/a/caa57b63-bb11-45e8-8795-de92ca61f4f1.jpg?1631303677";
        else if(id.equals("534832"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/3/832288fd-8031-4c2b-ad3e-b1ec9f94d379.jpg?1631331363";
        else if(id.equals("534833"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/3/832288fd-8031-4c2b-ad3e-b1ec9f94d379.jpg?1631331363";
        else if(id.equals("534836"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/9/999038b3-7d64-4554-b341-0675d4af8d8b.jpg?1630746254";
        else if(id.equals("534837"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/9/999038b3-7d64-4554-b341-0675d4af8d8b.jpg?1630746254";
        else if(id.equals("534846"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/b/ab17c8fa-4c06-4542-848a-e3f2f9f47c27.jpg?1631331448";
        else if(id.equals("534847"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/b/ab17c8fa-4c06-4542-848a-e3f2f9f47c27.jpg?1631331448";
        else if(id.equals("534852"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/b/7b63f2ae-5bfd-452f-b1f5-8459bcecd3bb.jpg?1631471600";
        else if(id.equals("534853"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/b/7b63f2ae-5bfd-452f-b1f5-8459bcecd3bb.jpg?1631471600";
        else if(id.equals("534860"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/d/5db99746-8aee-42b8-acb0-ed69933d0ff8.jpg?1631300153";
        else if(id.equals("534861"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/d/5db99746-8aee-42b8-acb0-ed69933d0ff8.jpg?1631300153";
        else if(id.equals("534863"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/a/0a3c8532-92f5-41db-92b4-a871aa05e0c7.jpg?1631136026";
        else if(id.equals("534864"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/a/0a3c8532-92f5-41db-92b4-a871aa05e0c7.jpg?1631136026";
        else if(id.equals("534870"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/b/bbdad18e-e262-41f9-b252-1cbdcdd1b5f9.jpg?1631304649";
        else if(id.equals("534871"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/b/bbdad18e-e262-41f9-b252-1cbdcdd1b5f9.jpg?1631304649";
        else if(id.equals("534875"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/a/daa2a273-488f-4285-a069-ad159ad2d393.jpg?1630695958";
        else if(id.equals("534876"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/a/daa2a273-488f-4285-a069-ad159ad2d393.jpg?1630695958";
        else if(id.equals("534877"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/6/e6dd05f0-a3c0-4bd6-a1d1-a74540623093.jpg?1631136104";
        else if(id.equals("534878"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/e/6/e6dd05f0-a3c0-4bd6-a1d1-a74540623093.jpg?1631136104";
        else if(id.equals("534882"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/f/0f6e668d-2502-4e82-b4c2-ef34c9afa27e.jpg?1631226866";
        else if(id.equals("534883"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/0/f/0f6e668d-2502-4e82-b4c2-ef34c9afa27e.jpg?1631226866";
        else if(id.equals("534894"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/5/55f0666a-5c3e-492b-b4ea-42fa7f24661b.jpg?1631300425";
        else if(id.equals("534895"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/5/55f0666a-5c3e-492b-b4ea-42fa7f24661b.jpg?1631300425";
        else if(id.equals("534901"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/4/d4054ae6-0227-4d99-8cb5-72e8b5d0b726.jpg?1631223080";
        else if(id.equals("534902"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/4/d4054ae6-0227-4d99-8cb5-72e8b5d0b726.jpg?1631223080";
        else if(id.equals("534915"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/2/d2feb859-bfae-4bc4-8181-5737dd5c3b08.jpg?1631352232";
        else if(id.equals("534916"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/2/d2feb859-bfae-4bc4-8181-5737dd5c3b08.jpg?1631352232";
        else if(id.equals("534918"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/e/be91fcba-4599-4ecb-824d-55112096c34a.jpg?1630693670";
        else if(id.equals("534919"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/b/e/be91fcba-4599-4ecb-824d-55112096c34a.jpg?1630693670";
        else if(id.equals("534921"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/5/35fdb976-291c-4824-9518-dd8c9f93fcde.jpg?1631300535";
        else if(id.equals("534922"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/5/35fdb976-291c-4824-9518-dd8c9f93fcde.jpg?1631300535";
        else if(id.equals("534936"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a33af331-0746-4adf-935a-bf61ff9d8d4b.jpg?1631134153";
        else if(id.equals("534937"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/3/a33af331-0746-4adf-935a-bf61ff9d8d4b.jpg?1631134153";
        else if(id.equals("534939"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/1/41b6381f-4ff8-49e9-bf00-cfe32851318b.jpg?1631222417";
        else if(id.equals("534940"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/4/1/41b6381f-4ff8-49e9-bf00-cfe32851318b.jpg?1631222417";
        else if(id.equals("534941"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/2/d2a5b43d-e21b-4294-9ea2-5bd0264e71d3.jpg?1631134183";
        else if(id.equals("534942"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/2/d2a5b43d-e21b-4294-9ea2-5bd0264e71d3.jpg?1631134183";
        else if(id.equals("534945"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/d/1d7b2d05-ce5c-4b73-8fa6-d9b69619d58c.jpg?1630658686";
        else if(id.equals("534946"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/d/1d7b2d05-ce5c-4b73-8fa6-d9b69619d58c.jpg?1630658686";
        else if(id.equals("534948"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/b/1bf48d2b-eb68-4f47-a80a-4751a4fa20a7.jpg?1630658693";
        else if(id.equals("534949"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/b/1bf48d2b-eb68-4f47-a80a-4751a4fa20a7.jpg?1630658693";
        else if(id.equals("534953"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/1/71ccc444-54c8-4f7c-a425-82bc3eea1eb0.jpg?1631797629";
        else if(id.equals("534954"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/1/71ccc444-54c8-4f7c-a425-82bc3eea1eb0.jpg?1631797629";
        else if(id.equals("534959"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/8/3849ad37-f80d-4ffc-9240-25a63326b3dd.jpg?1630958340";
        else if(id.equals("534960"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/8/3849ad37-f80d-4ffc-9240-25a63326b3dd.jpg?1630958340";
        else if(id.equals("534967"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/2/a2cda10b-7cd5-4cf5-87bd-c3b8c6aa2b47.jpg?1631376653";
        else if(id.equals("534968"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/2/a2cda10b-7cd5-4cf5-87bd-c3b8c6aa2b47.jpg?1631376653";
        else if(id.equals("534974"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/8/28e2119b-ed78-4b98-a956-f2b453d0b164.jpg?1631134932";
        else if(id.equals("534975"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/2/8/28e2119b-ed78-4b98-a956-f2b453d0b164.jpg?1631134932"; 
        else if(id.equals("534978"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/0/60e53d61-fcc3-4def-8206-052b46f62deb.jpg?1631314339";
        else if(id.equals("534979"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/6/0/60e53d61-fcc3-4def-8206-052b46f62deb.jpg?1631314339";
        else if(id.equals("534992"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/e/3e96f9a6-c215-42b1-aa02-8e6143fe5bd7.jpg?1631305272";
        else if(id.equals("534993"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/e/3e96f9a6-c215-42b1-aa02-8e6143fe5bd7.jpg?1631305272";
        else if(id.equals("534994"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/9/3983a304-5040-4b8d-945a-bf4ede3104a8.jpg?1631300854";
        else if(id.equals("534995"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/9/3983a304-5040-4b8d-945a-bf4ede3104a8.jpg?1631300854";
        else if(id.equals("535002"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/0/50d4b0df-a1d8-494f-a019-70ce34161320.jpg?1630658672";
        else if(id.equals("535003"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/5/0/50d4b0df-a1d8-494f-a019-70ce34161320.jpg?1630658672"; 
        else if(id.equals("535009"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/5/35cf2d72-931f-47b1-a1b4-916f0383551a.jpg?1631226297";
        else if(id.equals("535010"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/3/5/35cf2d72-931f-47b1-a1b4-916f0383551a.jpg?1631226297";
        else if(id.equals("535011"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/6/965e6bd5-dc32-406c-bc99-ceb15be4d3f2.jpg?1630828263";
        else if(id.equals("535012"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/9/6/965e6bd5-dc32-406c-bc99-ceb15be4d3f2.jpg?1630828263";
        else if(id.equals("535025"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/a/8ab5f2e6-0e0a-4f7d-a959-3d07948ff317.jpg?1631331050";
        else if(id.equals("535026"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/8/a/8ab5f2e6-0e0a-4f7d-a959-3d07948ff317.jpg?1631331050";
        else if(id.equals("535028"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/8/788288f6-7944-48f4-91b0-f452e209c9ce.jpg?1631607468";
        else if(id.equals("535029"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/7/8/788288f6-7944-48f4-91b0-f452e209c9ce.jpg?1631607468";
        else if(id.equals("535042"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/9/f953fad3-0cd1-48aa-8ed9-d7d2e293e6e2.jpg?1631607223";
        else if(id.equals("535043"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/f/9/f953fad3-0cd1-48aa-8ed9-d7d2e293e6e2.jpg?1631607223"; 
        else if(id.equals("535053"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/115a9a44-131d-45f3-852a-40fd18e4afb6.jpg?1631055774";
        else if(id.equals("535054"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/1/1/115a9a44-131d-45f3-852a-40fd18e4afb6.jpg?1631055774";
        else if(id.equals("535062"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/c/ac83c27f-55d6-4e5a-93a4-febb0c183289.jpg?1631342331";
        else if(id.equals("535063"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/a/c/ac83c27f-55d6-4e5a-93a4-febb0c183289.jpg?1631342331";
        else if(id.equals("296820"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/8/d8b718d8-fca3-4b3e-9448-6067c8656a9a.jpg?1629844769";
        else if(id.equals("296821"))
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/back/d/8/d8b718d8-fca3-4b3e-9448-6067c8656a9a.jpg?1629844769";
        else if(id.equals("535002t") || id.equals("534994t") || id.equals("534995t") || id.equals("54047311t")) // Wolf 2/2
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/6/364e04d9-9a8a-49df-921c-7a9bf62dc731.jpg?1632411038";
        else if(id.equals("534882t")) // Human 1/1
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1632410326";
        else if(id.equals("534836t")) // Zombie 2/2 Decayed
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1632410550";
        else if (id.equals("540753t")) //Treasure
            cardurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/2/720f3e68-84c0-462e-a0d1-90236ccc494a.jpg?1562539782";
        
        return cardurl;
    }
    
    public String getSpecialTokenUrl(String id){
        String tokenurl = "";

        if(id.equals("296754t") || id.equals("296741t") || id.equals("296730t") || id.equals("296728t") || id.equals("296723t") || 
                id.equals("296696t") ||  id.equals("296697t") || id.equals("296606t")) //Squirrel 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/7/977ddd05-1aae-46fc-95ce-866710d1c5c6.jpg?1626092815";
        else if(id.equals("539344t")) // Spirit 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/8/c865bc02-0562-408c-b18e-0e66da906fc6.jpg?1632410368";
        else if(id.equals("540460t") || id.equals("540461t") || id.equals("540729t")) // Zombie 2/2 Decayed
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1632410550";
        else if(id.equals("540749t")) // Human 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1632410326";
        else if(id.equals("534963t")) // Ooze green X/X
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/a/faa10292-f358-48c1-a516-9a1eecf62b1d.jpg?1632410909";
        else if(id.equals("534938t")) // Elemental red X/X
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/4/c4052aed-981b-41d0-85f0-20c2599811ba.jpg?1632410707";
        else if(id.equals("534999t")) // Treefolk green X/X
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/4/94e4345b-61b1-4026-a01c-c9f2036c5c8a.jpg?1632410986";
        else if(id.equals("296713t")) //Bear 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/8/c879d4a6-cef5-48f1-8c08-f5b59ec850de.jpg?1562857282";
        else if(id.equals("296771t") || id.equals("296738t") || id.equals("540468t")) //Spider 1/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/1/01591603-d903-419d-9957-cf0ae7f79240.jpg?1563073166";
        else if(id.equals("296753t") || id.equals("296707t") || id.equals("296708t")) //Beast 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/6/06b5e4d2-7eac-4ee9-82aa-80a668705679.jpg?1625974919";
        else if(id.equals("296546t")) //Illusion 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/b/ebccb29b-8b69-4813-94bb-d96e117b609e.jpg?1563073051";
        else if(id.equals("296665t") || id.equals("296655t") || id.equals("296615t")) //Goblin 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1627700700";
        else if(id.equals("296562t") || id.equals("296559t") || id.equals("296539t")) //Crab 0/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/e/7ef7f37a-b7f5-45a1-8f2b-7097089ca2e5.jpg?1626092589";
        else if(id.equals("296793t") || id.equals("296786t")) //Phyrexian Germ 0/0
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b53e0681-603e-4180-bc86-3dadf214e61a.jpg?1626092621";
        else if(id.equals("296787t") || id.equals("296490t")) //Shapeshifter 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a33fda72-e61d-478f-bc33-ff1a23b5f45b.jpg?1563072987";
        else if(id.equals("296472t")) //Phyrexian Golem 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/b/7becaa04-f142-4163-9286-00018b95c4ca.jpg?1601138543";
        else if (id.equals("296763t") || id.equals("539412t") || id.equals("539375t") || id.equals("539360t")) //Zombie Army 0/0
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/4/c46d82e0-ef99-473c-a09c-8f552db759bf.jpg?1626093060";
        else if(id.equals("296601t")) //Rat 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/a/1a85fe9d-ef18-46c4-88b0-cf2e222e30e4.jpg?1562279130";
        else if (id.equals("296673t")) //Elemental 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/5/e5b57672-c346-42f5-ac3e-82466a13b957.jpg?1563073089";
        else if(id.equals("296739t") || id.equals("296516t") || id.equals("539416t")) //Human Soldier 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/9/d9cbf36e-4044-4f08-9bae-f0dcb2455716.jpg?1562086882";
        else if(id.equals("296488t")) //Sliver 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/e/dec96e95-5580-4110-86ec-561007ab0f1e.jpg?1562640084";
        else if(id.equals("296500t")) //Bird 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/9/5988dc9e-724f-4645-8769-b94c5ef631b9.jpg?1568003326";
        else if(id.equals("296737t")) //Elephant 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/d/2dbccfc7-427b-41e6-b770-92d73994bf3b.jpg?1562701986";
        else if(id.equals("296582t") || id.equals("296580t") || id.equals("296822t") || id.equals("539390t") || id.equals("539388t") || 
                id.equals("539387t") || id.equals("539384t") || id.equals("539383t") || id.equals("539382t") || id.equals("539377t") ||
                id.equals("539374t") || id.equals("539373t") || id.equals("539371t") || id.equals("539369t") || id.equals("539367t") || 
                id.equals("539366t") || id.equals("539362t")) //Zombie 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/a/8a73e348-5bf1-4465-978b-3f31408bade9.jpg?1561757530";
        else if(id.equals("121236t") || id.equals("296511t") || id.equals("296502t") || id.equals("296471t")) //Bird 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/8/e8a1b1f2-f067-4c8a-b134-4567e4d5a7c6.jpg?1561758355";
        else if (id.equals("380486t")) //Bird Enchantment 2/2
            tokenurl = "https://www.mtg.onl/static/4952002452e39de9aa2c98b1f0e3765f/4d406/BNG_4_Bird_U_2_2.jpg";
        else if (id.equals("52181t")) //Centaur Enchantment 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/8/985be507-6125-4db2-b99f-8b61149ffeeb.jpg?1562636802";
        else if (id.equals("262699t") || id.equals("262875t") || id.equals("262857t") || id.equals("53054t") || id.equals("539403t")) //Wolf 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/6/462ff49b-a004-4dab-a25b-65cb18c1bbec.jpg?1592672584";
        else if(id.equals("378445t")) //Gold
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/c/0ca10abd-8d9d-4c0b-9d33-c3516abdf6b3.jpg?1562857254";
        else if (id.equals("380482t")) //Satyr 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/a/baa93038-2849-4c26-ab4f-1d50d276659f.jpg?1562636879";
        else if (id.equals("184589t") || id.equals("3832t")) //Spirit */*
            tokenurl = "https://www.mtg.onl/static/5681f8f60f717fb528d0d728cab2bd78/4d406/PROXY_Spirit_B_Y_Y.jpg";
        else if (id.equals("368951t") || id.equals("426025t")) //Elemental */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/6/8676704a-419e-4a00-a052-bca2ad34ecae.jpg?1601138189";
        else if (id.equals("380487t") || id.equals("414506t")) //Zombie */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/5/95c59642-575f-4356-ae1a-20b90895545b.jpg?1561757615";
        else if (id.equals("539365t")) //Zombie */* blue
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/7/d791c7af-1ba7-45ab-ad0c-be9ebc9e51f9.jpg?1562542412";
        else if (id.equals("114917t") || id.equals("52353t")) //Wurm */*
            tokenurl = "https://cdn.shopify.com/s/files/1/0790/8591/products/Token-front-WURM2_ca71d4fd-916a-4757-a31f-2fd1d631d128_800x800.jpg?v=1587053386";
        else if(id.equals("455911t") || id.equals("294389t")) //Horror 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/9/79a71b52-58f9-4945-9557-0fbcbbf5a241.jpg?1561757432";
        else if(id.equals("234849t") || id.equals("366401t") || id.equals("366340t")
                || id.equals("366375t") || id.equals("460772t")) // Ooze */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/8/580d30c8-df27-422d-b73a-2b27caf598eb.jpg?1562639814";
        else if(id.equals("52973t")) //Rhino 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/3/1331008a-ae86-4640-b823-a73be766ac16.jpg?1562539801";
        else if (id.equals("48096t")) //Demon */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/c/9ce65279-fc41-40f8-86a0-fdec72a0d91f.jpg?1561757673";
        else if(id.equals("383290t")) //Treefolk Warrior */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/5/2569593a-d2f2-414c-9e61-2c34e8a5832d.jpg?1562639718";
        else if(id.equals("51984t")) //Vampire Knight 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/9/8989fdb4-723b-4c80-89b4-930ccac13b22.jpg?1562086874";
        else if(id.equals("439331t")) //Wolf 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/5/a53f8031-aaa8-424c-929a-5478538a8cc6.jpg?1562639960";
        else if(id.equals("52494t") || id.equals("293206t") || id.equals("294605t")) //Golem 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/0/509be7b3-490d-4229-ba10-999921a6b977.jpg?1562841177";
        else if(id.equals("294598t")) //Myr 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/b/dbad9b20-0b13-41b9-a84a-06b691ee6c71.jpg?1562542415";
        else if(id.equals("423817t") || id.equals("423700t") || id.equals("183017t") || id.equals("383129t") ||
                id.equals("6164t") || id.equals("456522t") || id.equals("456545t") || id.equals("397624t") ||
                id.equals("52637t") || id.equals("52945t") || id.equals("53460t") || id.equals("53473t") ||
                id.equals("420600t") || id.equals("294436t") || id.equals("489333t") || id.equals("495977t") ||
                id.equals("295775t") || id.equals("295714t") || id.equals("295698t") || id.equals("295635t") ||
                id.equals("296365t") || id.equals("296532t") || id.equals("296482t") ||
                id.equals("296470t")) // Thopter 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/a/5a4649cc-07fb-4ff0-9ac6-846763b799df.jpg?1561757203";
        else if (id.equals("53057t") || id.equals("425825t")) //Wurm T1 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/6/b68e816f-f9ac-435b-ad0b-ceedbe72447a.jpg?1598312203";
        else if(id.equals("140233t") || id.equals("191239t") || id.equals("205957t") || id.equals("423797t") ||
                id.equals("51861t")) //Avatar */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/6/863768b5-3cf9-415c-b4fd-371dc5afee18.jpg?1561757503";
        else if (id.equals("53461t")) //Contruct 6/12
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/9/8936efa7-c4d0-426d-977b-38c957a9f025.jpg?1592710123";
        else if (id.equals("185704t")) //Vampire */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/6/969eff58-d91e-49e2-a1e1-8f32b4598810.jpg?1562636856";
        else if(id.equals("78975t")) //Snake 1/1 green
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/3/032e9f9d-b1e5-4724-9b80-e51500d12d5b.jpg?1562639651";
        else if(id.equals("296823t")) //Snake 1/1 black
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/3/032e9f9d-b1e5-4724-9b80-e51500d12d5b.jpg?1562639651";
        else if(id.equals("294401t")) //Dragon 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/b/0bb628da-a02f-4d3e-b919-0c03821dd5f2.jpg?1561756633";
        else if (id.equals("175105t") || id.equals("295412t")) //Beast 8/8
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/7/a7382e4b-43dc-4b35-8a9e-ab886ea0a981.jpg?1562636807";
        else if (id.equals("376496t") || id.equals("376549t") || id.equals("294519t")) //Thopter blue 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a3506ee6-a168-49a4-9814-2858194be60e.jpg?1592710025";
        else if (id.equals("247202t")) //Elemental 5/5
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/6/66029f69-2dc3-44e3-aa0d-4fe9a33b06f5.jpg?1625975207";
        else if (id.equals("376546t")) //Elemental 1/1 haste
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/4/94c14f3d-1578-426b-b64b-07c7e88ab572.jpg?1562279135";
        else if (id.equals("244668t")) //Faerie Rogue 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/0/a07b4786-1592-42c7-9d3e-d0d66abaed99.jpg?1562279139";
        else if(id.equals("294507t")) // Giant Warrior 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/0/a06eea30-810b-4623-9862-ec71c4bed11a.jpg?1562841186";
        else if(id.equals("294514t")) //Elf Warrior 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/b/cb8caa61-e294-4501-b357-a44abd77d09a.jpg?1601138497";
        else if (id.equals("457111t") || id.equals("51931t")) //Rogue 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/7/67457137-64f2-413d-b62e-658b3f1b1043.jpg?1547509251";
        else if (id.equals("376578t") || id.equals("152553t")) //Elemental 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/e/fea0857b-0f9e-4a87-83d7-85723e33f26c.jpg?1560081229";
        else if (id.equals("153166t")) //Merfolk Wizard 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/2/526da544-23dd-42b8-8c00-c3609eea4489.jpg?1562636751";
        else if(id.equals("83236t") || id.equals("45390t") || id.equals("965t") || id.equals("966t") ||
                id.equals("52750t")) //Rukh 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b5489e26-6aec-4706-9c3e-8454878fa6c3.jpg?1561757879";
        else if(id.equals("294426t")) //Spirit Warrior */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/e/febc7ce0-387f-413c-a387-2952b990ff3f.jpg?1562640159";
        else if (id.equals("19878t")) //Monkey 2/2
            tokenurl = "https://www.mtg.onl/static/9ce248147e36a52ccc388b3e642839aa/4d406/PROXY_Ape_G_2_2.jpg";
        else if (id.equals("126166t")) //Elf Druid 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/458f44dd-83f1-497e-b5d0-e3417eb9dfec.jpg?1592672533";
        else if (id.equals("202474t") || id.equals("1098t") || id.equals("2024t") || id.equals("3766t") || id.equals("11183t") || id.equals("902t")) //Djinn 5/5
            tokenurl = "https://media.mtgsalvation.com/attachments/71/116/635032489341076803.jpg";
        else if (id.equals("202590t") || id.equals("2073t") || id.equals("1027t")) // Tetravite
            tokenurl = "https://www.mtg.onl/static/a1f89472f590ea4e9652fe9dfebc1364/4d406/PROXY_Tetravite_1_1.jpg";
        else if (id.equals("3809t") || id.equals("2792t") || id.equals("1422t") || id.equals("159826t")) //Snake Artifact 1/1
            tokenurl = "https://www.mtg.onl/static/b19119feebdd5bed147282d3c643fca9/4d406/PROXY_Snake_1_1.jpg";
        else if (id.equals("407540t") || id.equals("407672t") || id.equals("407525t") || id.equals("293194t")) //Kor Ally 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/e/be224180-a482-4b94-8a9d-3a92ee0eb34b.jpg?1562640020";
        else if (id.equals("460768t")) //Lizard 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/5/e54486a4-f432-4e50-8639-799e036d0657.jpg?1625975014";
        else if (id.equals("201124t") || id.equals("3118t")) //Starfish 0/1
            tokenurl = "https://www.mtg.onl/static/536f2ee747044be2a06a820132a4b596/4d406/PROXY_Starfish_U_0_1.jpg";
        else if (id.equals("184730t") || id.equals("3192t") || id.equals("3193t")) //Knight Banding 1/1
            tokenurl = "https://www.mtg.onl/static/c88f42f8bd5a7c25aa36902546b690f5/4d406/PROXY_Knight_W_1_1.jpg";
        else if (id.equals("25910t")) //Angel 3/3
            tokenurl = "https://www.mtg.onl/static/9b6aafa10fefb5d5e55c6e4d2c1e512c/4d406/PROXY_Angel_B_3_3.jpg";
        else if (id.equals("6142t")) //Beast 2/2
            tokenurl = "https://www.mtg.onl/static/8eed0c2bcb05f3e26cdcc2f3f41d7f42/4d406/PROXY_Beast_G_2_2.jpg";
        else if (id.equals("34929t")) //Cat 1/1
            tokenurl = "https://www.mtg.onl/static/f23f6e35a23174a7fa9106d67d32fef9/4d406/PROXY_Cat_R_1_1.jpg";
        else if (id.equals("1649t") || id.equals("201182t")) //Minor Demon 1/1
            tokenurl = "https://www.mtg.onl/static/ebecf2ca03dfc9e71cc28e6df6b864bb/4d406/PROXY_Minor_Demon_BR_1_1.jpg";
        else if (id.equals("4854t") || id.equals("376556t")) //Carnivore 3/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/4/8437b125-6057-4d17-9f2c-28ea56553f84.jpg?1562702178";
        else if (id.equals("4771t")) //Dog 1/1
            tokenurl = "https://www.cardkingdom.com/images/magic-the-gathering/core-set-2021/dog-token-55913-medium.jpg";
        else if (id.equals("9667t")) //Giant Bird 4/4
            tokenurl = "https://www.mtg.onl/static/abe5178af8ebbe84f5504493a1b5f154/4d406/PROXY_Giant_Chicken_R_4_4.jpg";
        else if (id.equals("74265t")) //Expansion Symbol 1/1
            tokenurl = "https://www.mtg.onl/static/9de9c9d3d17a3a8eb20c9c66b5b9253a/4d406/PROXY_ExpansionSymbol_1_1.jpg";
        else if (id.equals("73953t")) //Giant Teddy 5/5
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/2/628c542b-7579-4070-9143-6f1f7221468f.jpg?1609977149";
        else if (id.equals("25956t")) //Kavu 3/4
            tokenurl = "https://www.mtg.onl/static/740ce087c4aff57e881b01c28528c8f9/4d406/PROXY_Kavu_B_3_3.jpg";
        else if (id.equals("184598t") || id.equals("2959t")) //Kelp 0/1
            tokenurl = "http://magicplugin.normalitycomics.com/cardimages/lackey/kelp-u-0-1-defender-v4.jpg";
        else if (id.equals("111046t")) //Insect 6/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/f/0ff2e2bd-b8e9-4563-85ad-fdbb0607fb7c.jpg?1619404736";
        else if (id.equals("27634t") || id.equals("3227t") || id.equals("159097t") || id.equals("294453t")) //Hippo 1/1
            tokenurl = "https://www.mtg.onl/static/8b684bdea239d594e296a134f5ec1783/4d406/PROXY_Hippo_G_1_1.jpg";
        else if (id.equals("3148t")) //Splinter 1/1
            tokenurl = "https://www.mtg.onl/static/73cad75db99d3ba716082464bfd85b2e/4d406/PROXY_Splinter_G_1_1.jpg";
        else if(id.equals("26815t") || id.equals("51774t")) //Cat 2/1
            tokenurl = "https://www.mtg.onl/static/8bb68cf125fdcc9d8a21b3dade2f11cb/4d406/PROXY_Cat_B_2_1.jpg";
        else if (id.equals("1534t")) //Wolves of the Hunt 1/1
            tokenurl = "https://www.mtg.onl/static/e34edc351ea7ef08c4c4064d1f890731/4d406/PROXY_Wolves_of_the_Hunt_G_1_1.jpg";
        else if (id.equals("130314t")) //Zombie Goblin 1/1
            tokenurl = "https://www.mtg.onl/static/334463a009d3b5b3068eaf61621870ef/4d406/PROXY_Festering_Goblin_B_1_1.jpg";
        else if (id.equals("116383t")) //Bat 1/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/c/4c532e0f-8934-4ad3-bb1a-640abe946e10.jpg?1619404562";
        else if (id.equals("124344t")) //Cat Warrior 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/9/29c4e4f2-0040-4490-b357-660d729ad9cc.jpg?1562636772";
        else if (id.equals("376404t")) //Elemental */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/b/db67bc06-b6c9-49a0-beef-4d35842497cb.jpg?1561929912";
        else if (id.equals("409810t") || id.equals("409805t") || id.equals("409953t") || id.equals("409997t") || 
                id.equals("410032t")  || id.equals("293377t") || id.equals("294345t") || id.equals("295471t")) //Clue
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/2/f2c859e1-181e-44d1-afbd-bbd6e52cf42a.jpg?1562086885";
        else if (id.equals("3242t")) //Wall 0/2
            tokenurl = "https://www.mtg.onl/static/18f8f17bbe1f81822efa4bed878b6437/4d406/PROXY_Wall_0_2.jpg";
        else if (id.equals("21382t")) //Elephant */*
            tokenurl = "https://www.mtg.onl/static/b740cce52030bca3b02d2a917152314f/4d406/PROXY_Elephant_G_Y_Y.jpg";
        else if (id.equals("293348t") || id.equals("293058t")) //Eldrazi Horror 3/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/11d25bde-a303-4b06-a3e1-4ad642deae58.jpg?1562636737";
        else if (id.equals("416746t")) //Marit Lage 20/20
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/b/fb248ba0-2ee7-4994-be57-2bcc8df29680.jpg?1598311645";
        else if (id.equals("46168t")) // Construct */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/5/c5eafa38-5333-4ef2-9661-08074c580a32.jpg?1562702317";
        else if(id.equals("423843t") || id.equals("423739t") || id.equals("423718t") || id.equals("423736t") ||
                id.equals("423691t") || id.equals("423743t") || id.equals("423769t") || id.equals("423670t") ||
                id.equals("423796t") || id.equals("423680t") || id.equals("423693t") || id.equals("52046t")  ||
                id.equals("52791t")  || id.equals("53426t")  || id.equals("53432t")  || id.equals("294273t") ||
                id.equals("293046t") || id.equals("293107t") || id.equals("293548t") || id.equals("294419t") ||
                id.equals("295769t") || id.equals("295726t") || id.equals("295719t") || id.equals("295696t") ||
                id.equals("295675t") || id.equals("295673t") || id.equals("295661t") || id.equals("295612t") ||
                id.equals("295609t") || id.equals("295605t") || id.equals("295598t") || id.equals("295597t") ||
                id.equals("295574t") || id.equals("295538t") || id.equals("295535t") || id.equals("295532t") ||
                id.equals("295529t") || id.equals("295525t") || id.equals("295524t") || id.equals("295520t") ||
                id.equals("295513t") || id.equals("295506t") || id.equals("295502t") || id.equals("293737t")) //Servo 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/7/d79e2bf1-d26d-4be3-a5ad-a43346ed445a.jpg?1562640071";
        else if (id.equals("265141t")) //Boar 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/3/83dcacd3-8707-4354-a1a5-9863d677d67f.jpg?1562702177";
        else if(id.equals("383077t")) //Saproling */*
            tokenurl = "https://www.mtg.onl/static/018b0db17f54cdd63bd182174fe3ef5b/4d406/PROXY_Saproling_G_Y_Y.jpg";
        else if(id.equals("53274t")) //Bird 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/f/cf64f834-a645-4db4-a34f-9cab725fc1b1.jpg?1620531482";
        else if(id.equals("53244t")) //Soldier 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/a/0a47f751-52f1-4042-85dd-ea222e5d969d.jpg?1562896993";
        else if(id.equals("53240t") || id.equals("296593t") || id.equals("296479t")) //Spirit 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/9/4914610d-7d4f-4cf6-98db-c39e79cce25c.jpg?1562702037";
        else if(id.equals("53299t")) //Thopter Blue 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/e/eef8b4fc-238f-4c1f-ad98-a1769fd44eab.jpg?1598311587";
        else if(id.equals("53246t")) //Germ 0/0
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/1/61f94e32-3b22-4c47-b866-1f36a7f3c734.jpg?1562702081";
        else if(id.equals("53259t")) //Goblin 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/9/99a6ebce-f391-4642-857a-4dc1466895f3.jpg?1562926018";
        else if(id.equals("53264t")) //Lizard 8/8
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/0/70345006-5cde-44f8-ab66-9d8163d4c4f6.jpg?1561897499";
        else if(id.equals("53289t")) //Saproling 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/3/0302fa7d-2e34-4f4a-a84e-7a78febc77f5.jpg?1562895593";
        else if(id.equals("53300t")) //Construct 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/c/7c82af53-2de8-4cd6-84bf-fb39d2693de2.jpg?1561897501";
        else if (id.equals("401697t") || id.equals("401692t") || id.equals("401701t") || id.equals("293619t") || id.equals("294261t") || 
                id.equals("293585t") || id.equals("539400t")) // Eldrazi Spawn 0/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/7/7787eae2-7dfb-44ab-8e92-56fdfc0bb39e.jpg?1593142790";
        else if (id.equals("376397t") || id.equals("107557t")) //Drake Green Blue 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/0/c06d2c07-7d3e-46e3-86f0-7ceba3b0aee0.jpg?1592672602";
        else if(id.equals("52398t")) //Illusion 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/1/a10729a5-061a-4daf-91d6-0f6ce813a992.jpg?1562539791";
        else if (id.equals("435411t") || id.equals("435410t")) //Treasure
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/7/2/720f3e68-84c0-462e-a0d1-90236ccc494a.jpg?1562539782";
        else if (id.equals("1686t") || id.equals("2881t") || id.equals("201231t")) //Stangg Twin 3/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/b/eba90d37-d7ac-4097-a04d-1f27e4c9e5de.jpg?1562702416";
        else if (id.equals("439843t")) //Golem 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/7/a7820eb9-6d7f-4bc4-b421-4e4420642fb7.jpg?1561757771";
        else if(id.equals("447070t") || id.equals("53480t")) //Mowu 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/1/b10441dd-9029-4f95-9566-d3771ebd36bd.jpg?1626572250";
        else if(id.equals("53190t")) //Elemental 5/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/5/05aa19b7-da04-4845-868e-3ad2edb9a9ba.jpg?1562701877";
        else if (id.equals("452760t") || id.equals("296508t")) //Angel 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/c/acb271a8-68bb-45e6-9f99-568479e92ea0.jpg?1572892475";
        else if(id.equals("53453t")) //Mask Enchantment
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/2/b21b5504-c5ef-4dfc-8219-8db90aca7694.jpg?1592709997";
        else if(id.equals("53438t")) //Myr 2/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/8/483c8cd6-288c-49d7-ac28-642132f85259.jpg?1598311565";
        else if(id.equals("53463t")) //Survivor 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/4/f4478979-19b6-4524-bbbd-519594c38f5a.jpg?1592710055";
        else if(id.equals("52149t")) //Soldier 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/45907b16-af17-4237-ab38-9d7537fd30e8.jpg?1572892483";
        else if (id.equals("89110t") || id.equals("456379t")) //Voja 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/8/2879010f-b752-4808-8531-d24e612de0d9.jpg?1541006575";
        else if (id.equals("116384t") || id.equals("376564t") || id.equals("52993t")) //Assembly-Worker 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/7/e72daa68-0680-431c-a616-b3693fd58813.jpg?1619404806";
        else if(id.equals("17841t") || id.equals("17850t") || id.equals("17852t") || id.equals("19444t") || id.equals("294101t") || 
                id.equals("294226t")) //Elf Warrior 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/118d0655-5719-4512-8bc1-fe759669811b.jpg?1615686731";
        else if(id.equals("383392t") || id.equals("539394t")) //Beast 3/3
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/f/3fc3a29a-280d-4f2c-9a01-8cfead75f583.jpg?1561756988";
        else if (id.equals("5610t") || id.equals("416754t")) //Minion */*
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/9/a9930d11-4772-4fc2-abbd-9af0a9b23a3e.jpg?1561757789";
        else if (id.equals("5173t")) //Insect Artifact 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/4/54ec2cd6-51f6-4e12-af90-fa254f14ad32.jpg?1561757159";
        else if(id.equals("378521t") || id.equals("52418t")) //Kraken 9/9
            tokenurl= "https://c1.scryfall.com/file/scryfall-cards/large/front/d/0/d0cd85cc-ad22-446b-8378-5eb69fee1959.jpg?1562840712";
        else if(id.equals("52136t")) //Soldier 1/1
            tokenurl= "https://c1.scryfall.com/file/scryfall-cards/large/front/4/5/45907b16-af17-4237-ab38-9d7537fd30e8.jpg?1572892483";
        else if (id.equals("271158t") || id.equals("401703t")) //Hellion 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/a/da59fb40-b218-452f-b161-3bde15e30c74.jpg?1593142801";
        else if (id.equals("88973t") || id.equals("368549t")) //Spirit 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/3/b3c9a097-219b-4aaf-831f-cc0cddbcfaae.jpg?1561757870";
        else if (id.equals("53454t")) // Zombie 2/2
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1562844819";
        else if (id.equals("417465t") || id.equals("294137t") || id.equals("296576t")) //Eldrazi Scion 1/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/7/a7ba0398-35e1-4733-ad29-e853757d6f24.jpg?1562230140";
        else if (id.equals("417480t")) //Demon 5/5
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/4/545639fc-e521-41f2-81b2-a671007321eb.jpg?1562229670";
        else if (id.equals("417481t") || id.equals("293725t")) //Zombie Giant 5/5
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/e/be7e26e1-5db6-49ba-a88e-c79d889cd364.jpg?1561757964";
        else if (id.equals("417447t")) //Elemental 4/4
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/e/fea0857b-0f9e-4a87-83d7-85723e33f26c.jpg?1560081229";
        else if(id.equals("220535t") || id.equals("376253t") || id.equals("376390t") || id.equals("53439t") ||
                id.equals("401643t") || id.equals("417451t") || id.equals("417424t") || id.equals("51908t") ||
                id.equals("52593t") || id.equals("53161t") || id.equals("271227t")) // Plant 0/1
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/f/a/fa0025fa-c530-4151-bcff-48425a4f1db5.jpg?1562230616";
        else if(id.equals("3392t")) // Wood 0/1
            tokenurl = "https://www.mtg.onl/static/70c0c3608291aaee9517eff9cacd43d6/4d406/PROXY_Wood_G_0_1.jpg";
        else if (id.equals("21381t") || id.equals("40198t"))
            tokenurl = "https://img.scryfall.com/cards/large/back/8/c/8ce60642-e207-46e6-b198-d803ff3b47f4.jpg?1562921132";
        else if (id.equals("461099t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/d/e/de7ba875-f77b-404f-8b75-4ba6f81da410.jpg?1557575978";
        else if (id.equals("426909t") || id.equals("426705t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/8/98956e73-04e4-4d7f-bda5-cfa78eb71350.jpg?1562844807";
        else if (id.equals("426897t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/a/8/a8f339c6-2c0d-4631-849b-44d4360b5131.jpg?1562844814";
        else if (id.equals("457139t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/0/105e687e-7196-4010-a6b7-cfa42d998fa4.jpg?1560096976";
        else if (id.equals("470549t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/7/7/7711a586-37f9-4560-b25d-4fb339d9cd55.jpg?1565299650";
        else if (id.equals("113527t") || id.equals("376321t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/5/b/5b9f471a-1822-4981-95a9-8923d83ddcbf.jpg?1562702075";
        else if (id.equals("114919t") || id.equals("247519t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/b/5/b5ddb67c-82fb-42d6-a4c2-11cd38eb128d.jpg?1562702281";
        else if (id.equals("8862t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/d/b/dbf33cc3-254f-4c5c-be22-3a2d96f29b80.jpg?1562936030";
        else if(id.equals("213757t") || id.equals("213734t") || id.equals("221554t") || id.equals("48049t") ||
                id.equals("46160t") || id.equals("47450t") || id.equals("376421t") || id.equals("213725t") ||
                id.equals("52492t") || id.equals("489268t") || id.equals("489458t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/f/3/f32ad93f-3fd5-465c-ac6a-6f8fb57c19bd.jpg?1561758422";
        else if (id.equals("247393t") || id.equals("247399t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/f/1feaa879-ceb3-4b20-8021-ae41d8be9005.jpg?1562636755";
        else if (id.equals("152998t") || id.equals("152963t") || id.equals("52364t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/5/959ed4bf-b276-45ed-b44d-c757e9c25846.jpg?1562702204";
        else if (id.equals("46703t") || id.equals("227151t") || id.equals("205298t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/0/a/0a9a25fd-1a4c-4d63-bbfa-296ef53feb8b.jpg?1562541933";
        else if (id.equals("394380t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/6/2/622397a1-6513-44b9-928a-388be06d4022.jpg?1562702085";
        else if (id.equals("1138t") || id.equals("2074t") || id.equals("640t") || id.equals("3814t") || id.equals("11530t") ||
                id.equals("43t") || id.equals("338t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/c/7/c75b81b5-5c84-45d4-832a-20c038372bc6.jpg?1561758040";
        else if (id.equals("275261t") || id.equals("271156t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/f/1feaa879-ceb3-4b20-8021-ae41d8be9005.jpg?1562636755";
        else if (id.equals("376455t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/e/9e0eeebf-7c4a-436b-8cb4-292e53783ff2.jpg?1562926847";
        else if(id.equals("414388t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/b/8/b8710a30-8314-49ef-b995-bd05454095be.jpg?1562636876";
        else if(id.equals("382874t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/8/3/83dcacd3-8707-4354-a1a5-9863d677d67f.jpg?1562702177";
        else if(id.equals("383065t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/8/5/8597029c-3b0d-476e-a6ee-48402f815dab.jpg?1561757494";
        else if(id.equals("414350t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/e/4/e4439a8b-ef98-428d-a274-53c660b23afe.jpg?1562636929";
        else if(id.equals("414349t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/e/4/e4439a8b-ef98-428d-a274-53c660b23afe.jpg?1562636929";
        else if(id.equals("414429t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/d/b/dbd994fc-f3f0-4c81-86bd-14ca63ec229b.jpg?1562636922";
        else if(id.equals("414314t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/1/11d25bde-a303-4b06-a3e1-4ad642deae58.jpg?1562636737";
        else if(id.equals("414313t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/1/11d25bde-a303-4b06-a3e1-4ad642deae58.jpg?1562636737";
        else if(id.equals("227061t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/5/f/5f68c2ab-5131-4620-920f-7ba99522ccf0.jpg?1562639825";
        else if(id.equals("227072t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/5/f/5f68c2ab-5131-4620-920f-7ba99522ccf0.jpg?1562639825";
        else if(id.equals("245250t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/a/5/a53f8031-aaa8-424c-929a-5478538a8cc6.jpg?1562639960";
        else if(id.equals("245251t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/a/5/a53f8031-aaa8-424c-929a-5478538a8cc6.jpg?1562639960";
        else if(id.equals("398441t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/e/5/e5ccae95-95c2-4d11-aa68-5c80ecf90fd2.jpg?1562640112";
        else if (id.equals("409826t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/e/0/e0a12a72-5cd9-4f1b-997d-7dabb65e9f51.jpg?1562086884";
        else if (id.equals("51939t") || id.equals("52121t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1562640005";
        else if (id.equals("52110t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/0/b/0bb628da-a02f-4d3e-b919-0c03821dd5f2.jpg?1561756633";
        else if (id.equals("473141t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/b/f/bf36408d-ed85-497f-8e68-d3a922c388a0.jpg?1567710130";
        else if(id.equals("53180t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/1/f/1feaa879-ceb3-4b20-8021-ae41d8be9005.jpg?1562636755";
        else if(id.equals("53118t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/0/3/03553980-53fa-4256-b478-c7e0e73e2b5b.jpg?1563132220";
        else if(id.equals("53268t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/6/c/6c1ffb14-9d92-4239-8694-61d156c9dba7.jpg?1562254006";
        else if(id.equals("53403t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/a/e/ae196fbc-c9ee-4dba-9eb3-52209908b898.jpg?1561757813";
        else if(id.equals("53408t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/0/e/0e80f154-9409-40fa-a564-6fc296498d80.jpg?1562898335";
        else if(id.equals("53417t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/2/9/29c4e4f2-0040-4490-b357-660d729ad9cc.jpg?1562636772";
        else if(id.equals("53326t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/7/4/748d267d-9c81-4dc0-92b7-eafb7691c6cc.jpg?1562636817";
        else if(id.equals("16787t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/e/8/e8a56b33-f720-4cbf-8015-59b5fd8ff756.jpg?1562941690";
        else if(id.equals("16759t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/f/3/f3b5665e-2b97-47c7-bbf9-6549c2c8a9f2.jpg?1562944002";
        else if(id.equals("456382t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/b/6/b64c5f80-4676-4860-be0e-20bcf2227405.jpg?1562540215";
        else if(id.equals("460464t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/4/94ed2eca-1579-411d-af6f-c7359c65de30.jpg?1562086876";
        else if(id.equals("19461t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/d/2/d2f51f4d-eb6d-4503-b9a4-559db1b9b16f.jpg?1574710411";
        else if(id.equals("19471t") || id.equals("19472t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/3/4/340fb06f-4bb0-4d23-b08c-8b1da4a8c2ad.jpg?1574709457";
        else if(id.equals("294089t") || id.equals("294717t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/8/b/8b4f81e2-916f-4af4-9e63-f4469e953122.jpg?1562702183";
        else if(id.equals("293323t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/2/f/2f4b7c63-8430-4ca4-baee-dc958d5bd22f.jpg?1557575919";
        else if (id.equals("74492t"))
            tokenurl = "https://media.mtgsalvation.com/attachments/94/295/635032496473215708.jpg";
        else if (id.equals("3280t"))
            tokenurl = "https://media.mtgsalvation.com/attachments/54/421/635032484680831888.jpg";
        else if (id.equals("107091t") || id.equals("295407t"))
            tokenurl = "https://media.mtgsalvation.com/attachments/13/534/635032476540667501.jpg";
        else if (id.equals("184735t") || id.equals("376488t") || id.equals("3066t") || id.equals("121261t"))
            tokenurl = "https://i.pinimg.com/originals/a9/fb/37/a9fb37bdfa8f8013b7eb854d155838e2.jpg";
        else if (id.equals("205297t") || id.equals("50104t"))
            tokenurl = "https://i.pinimg.com/564x/cc/96/e3/cc96e3bdbe7e0f4bf1c0c1f942c073a9.jpg";
        else if (id.equals("3591t"))
            tokenurl = "https://i.pinimg.com/564x/6e/8d/fe/6e8dfeee2919a3efff210df56ab7b85d.jpg";
        else if (id.equals("136155t"))
            tokenurl = "https://i.pinimg.com/564x/5d/68/d6/5d68d67bef76bf90588a4afdc39dc60e.jpg";
        else if (id.equals("439538t"))
            tokenurl = "https://i.pinimg.com/originals/da/e3/31/dae3312aa1f15f876ebd363898847e23.jpg";
        else if(id.equals("397656t"))
            tokenurl = "https://i.pinimg.com/originals/3c/f4/55/3cf45588a840361b54a95141b335b76c.jpg";
        else if(id.equals("51789t") || id.equals("52682t"))
            tokenurl = "https://i.pinimg.com/originals/4c/40/ae/4c40ae9a4a4c8bb352b26bea0f277a26.jpg";
        else if (id.equals("3421t") || id.equals("15434t"))
            tokenurl = "https://www.mtg.onl/static/3c152b4fc1c64e3ce21022f53ec16559/4d406/PROXY_Cat_G_1_1.jpg";
        else if (id.equals("73976t"))
            tokenurl = "https://www.mtg.onl/static/8bbca3c195e798ca92b4a112275072e2/4d406/PROXY_Ape_G_1_1.jpg";
        else if (id.equals("49026t"))
            tokenurl = "https://www.mtg.onl/static/a9d81341e62e39e75075b573739f39d6/4d406/PROXY_Wirefly_2_2.jpg";
        else if (id.equals("3449t"))
            tokenurl = "https://www.mtg.onl/static/8c7fed1a0b8edd97c0fb0ceab24a654f/4d406/PROXY_Goblin_Scout_R_1_1.jpg";
        else if (id.equals("24624t"))
            tokenurl = "https://www.mtg.onl/static/6d717cba653ea9e3f6bd1419741671cb/4d406/PROXY_Minion_B_1_1.jpg";
        else if (id.equals("89051t") || id.equals("519129t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/0/a0b5e1f4-9206-40b6-9cf6-331f6a95d045.jpg?1618767339";
        else if (id.equals("72858t"))
            tokenurl = "https://www.mtg.onl/static/348314ede9097dd8f6dd018a6502d125/4d406/PROXY_Pincher_2_2.jpg";
        else if (id.equals("3113t"))
            tokenurl = "https://www.mtg.onl/static/fca7508d78c26e3daea78fd4640faf9a/4d406/PROXY_Orb_U_X_X.jpg";
        else if (id.equals("74027t"))
            tokenurl = "https://www.mtg.onl/static/48515f01d0fda15dd9308d3a528dae7b/4d406/PROXY_Spirit_W_3_3.jpg";
        else if (id.equals("23319t"))
            tokenurl = "https://www.mtg.onl/static/0f8b0552293c03a3a29614cc83024337/4d406/PROXY_Reflection_W_X_X.jpg";
        else if (id.equals("130638t"))
            tokenurl = "https://www.mtg.onl/static/20b01e1378e7b8e8b47066c52761fde2/4d406/PROXY_Giant_R_4_4.jpg";
        else if (id.equals("74411t"))
            tokenurl = "https://www.mtg.onl/static/5f65ea90850736160a28f3a5bd56744a/4d406/PROXY_Warrior_R_1_1.jpg";
        else if (id.equals("121156t"))
            tokenurl = "https://www.mtg.onl/static/3db04e8bdd45aac4bb25bb85cdb05ac0/4d406/PROXY_Wolf_G_1_1.jpg";
        else if (id.equals("126816t"))
            tokenurl = "https://www.mtg.onl/static/e25f8b900e6238d0047039da4690f1c4/4d406/PROXY_Knight_B_2_2.jpg";
        else if (id.equals("75291t"))
            tokenurl = "http://4.bp.blogspot.com/-y5Fanm3qvrU/Vmd4gGnl2DI/AAAAAAAAAWY/FCrS9FTgOJk/s1600/Tatsumasa%2BToken.jpg";
        else if (id.equals("26732t"))
            tokenurl = "http://1.bp.blogspot.com/-0-mLvfUVgNk/VmdZWXWxikI/AAAAAAAAAUM/TVCIiZ_c67g/s1600/Spawn%2BToken.jpg";
        else if (id.equals("47449t") || id.equals("52335t") || id.equals("295457t"))
            tokenurl = "https://1.bp.blogspot.com/-vrgXPWqThMw/XTyInczwobI/AAAAAAAADW4/SEceF3nunBkiCmHWfx6UxEUMF_gqdrvUQCLcBGAs/s1600/Kaldra%2BToken%2BUpdate.jpg";
        else if(id.equals("460140t") || id.equals("460146t"))
            tokenurl = "http://4.bp.blogspot.com/-jmiOVll5hDk/VmdvG_Hv7hI/AAAAAAAAAVg/oWYbn2yBPI8/s1600/White-Blue%2BBird%2BToken.jpg";
        else if (id.equals("5261t"))
            tokenurl = "https://static.cardmarket.com/img/5a0199344cad68eebeefca6fa24e52c3/items/1/MH1/376905.jpg";
        else if (id.equals("430686t"))
            tokenurl = "https://cdn.shopify.com/s/files/1/1601/3103/products/Token_45_2000x.jpg?v=1528922847";
        else if (id.equals("405191t"))
            tokenurl = "https://6d4be195623157e28848-7697ece4918e0a73861de0eb37d08968.ssl.cf1.rackcdn.com/108181_200w.jpg";
        else if (id.equals("476402t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/6/0/60466c78-155e-442b-8022-795e1e9de8df.jpg?1581901998";
        else if(id.equals("484904t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/2/1/21e89101-f1cf-4bbd-a1d5-c5d48512e0dd.jpg?1562539760";
        else if(id.equals("489168t"))
            tokenurl="https://img.scryfall.com/cards/large/front/d/e/dee1c2ee-d92e-409a-995a-b4c91620c918.jpg?1581901969";
        else if(id.equals("489401t"))
            tokenurl="https://i.pinimg.com/564x/01/b0/a2/01b0a289e1a28167cbf0f30532328d99.jpg";
        else if(id.equals("489171t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/4/f/4f8107b3-8539-4b9c-8d0d-c512c940838f.jpg?1592324480";
        else if(id.equals("489403t") || id.equals("489358t") || id.equals("489372t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/5/959ed4bf-b276-45ed-b44d-c757e9c25846.jpg";
        else if(id.equals("489562t") || id.equals("296282"))
            tokenurl="https://img.scryfall.com/cards/large/front/c/f/cf9a289f-cd3f-42a0-9296-8c7cc7d01a91.jpg?1561758108";
        else if(id.equals("489363t"))
            tokenurl="https://img.scryfall.com/cards/large/front/8/3/83dcacd3-8707-4354-a1a5-9863d677d67f.jpg?1562702177";
        else if(id.equals("489900t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/8/6/8676704a-419e-4a00-a052-bca2ad34ecae.jpg?1596044091";
        else if(id.equals("489695t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/7/b/7becaa04-f142-4163-9286-00018b95c4ca.jpg?1596044169";
        else if(id.equals("489907t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/9/e/9ecc467e-b345-446c-b9b7-5f164e6651a4.jpg?1596043489";
        else if(id.equals("295082t"))
            tokenurl = "https://img.scryfall.com/cards/large/front/a/e/ae56d9e8-de05-456b-af32-b5992992ee15.jpg?1562639978";
        else if(id.equals("496035t") || id.equals("295423t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/d/ed666385-a2e7-4e1f-ad2c-babbfc0c50b3.jpg?1562640123";
        else if(id.equals("496036t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/1/e1eb3b8a-f9d3-4ce1-b171-ba7b0c3f4830.jpg?1562702405";
        else if(id.equals("495984t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/3/33bd708d-dc84-44d3-a563-536ade028bd0.jpg?1562702016";
        else if(id.equals("495971t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/7/67457137-64f2-413d-b62e-658b3f1b1043.jpg?1547509251";
        else if(id.equals("495958t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/b/e/be224180-a482-4b94-8a9d-3a92ee0eb34b.jpg?1562640020";
        else if(id.equals("295356t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/7/c7e7822b-f155-4f3f-b835-ec64f3a71307.jpg?1601138813";
        else if(id.equals("295376t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/b/cb8caa61-e294-4501-b357-a44abd77d09a.jpg?1601138497";
        else if(id.equals("295334t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/c/dcee70ef-6285-4f09-8a71-8b7960e8fa99.jpg?1562636925";
        else if(id.equals("295433t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/f/2f4b7c63-8430-4ca4-baee-dc958d5bd22f.jpg?1557575919";
        else if(id.equals("295428t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/5/3/5371de1b-db33-4db4-a518-e35c71aa72b7.jpg?1562702067";
        else if(id.equals("295377t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/e/ce90c48f-74fb-4e87-9e46-7f8c3d79cbb0.jpg?1562636904";
        else if(id.equals("295322t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/0/903e30f3-580e-4a14-989b-ae0632363407.jpg?1581902165";
        else if(id.equals("295234t"))
            tokenurl ="https://c1.scryfall.com/file/scryfall-cards/large/front/d/c/dc77b308-9d0c-492f-b3fe-e00d60470767.jpg?1563073222";
        else if(id.equals("295225t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/9/d9c95045-e806-4933-94a4-cb52ae1a215b.jpg?1562542413";
        else if(id.equals("295217t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/4/0419a202-6e32-4f0a-a032-72f6c00cae5e.jpg?1562639654";
        else if(id.equals("295556t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/2/623a08d1-f5ff-48b7-bdb6-54b8d7a4b931.jpg?1562639829";
        else if(id.equals("503330t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/5/c5ad13b4-bbf5-4c98-868f-4d105eaf8833.jpg?1592710082";
        else if(id.equals("503754t") || id.equals("503827t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/a/4ae9f454-4f8c-4123-9886-674bc439dfe7.jpg?1611206933";
        else if(id.equals("503846t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/d/3db39e3b-fad4-4c9b-911f-69883ac7e0e1.jpg?1611206884";
        else if(id.equals("503821t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/f/ef775ad0-b1a9-4254-ab6f-304558bb77a1.jpg?1611247501";
        else if(id.equals("508147t") || id.equals("508338t") || id.equals("508160t") || id.equals("508357t") || 
                id.equals("508354t") || id.equals("508349t") || id.equals("508343t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/118d0655-5719-4512-8bc1-fe759669811b.jpg?1611206824";
        else if(id.equals("295919t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/1/118d0655-5719-4512-8bc1-fe759669811b.jpg?1611206824";
        else if(id.equals("518457t") || id.equals("518473t") || id.equals("518468t") || id.equals("518463t") || id.equals("518460t") ||
                id.equals("518422t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/1/910f48ab-b04e-4874-b31d-a86a7bc5af14.jpg?1617626097";
        else if(id.equals("518467t") || id.equals("518410t") || id.equals("518436t") || id.equals("518308t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/9/c9deae5c-80d4-4701-b425-91853b7ee03b.jpg?1617626050";
        else if(id.equals("518461t") || id.equals("518432t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/d/0/d0ddbe3e-4a66-494d-9304-7471232549bf.jpg?1617626099";
        else if(id.equals("518310t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/d/3d0b9b88-705e-4df0-8a93-3e240b81355b.jpg?1617626092";
        else if(id.equals("513663t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/1/a/1a2d027f-8996-4761-a776-47cd428f6779.jpg?1618766925";
        else if(id.equals("522245t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/3/7/37e32ba6-108a-421f-9dad-3d03f7ebe239.jpg?1623113548";
        else if(id.equals("296413t"))
            tokenurl = "https://i.pinimg.com/564x/af/cc/4c/afcc4c87d67c9651838fed09217c7eed.jpg";
        else if(id.equals("296410t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/4/94057dc6-e589-4a29-9bda-90f5bece96c4.jpg?1572489125";
        else if(id.equals("527539t") || id.equals("527477t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a3a684b7-27e0-4d9e-a064-9e03c6e50c89.jpg?1626139418";
        else if(id.equals("527351t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/c/e/ce3c0bd9-8a37-4164-9937-f35d1c210fe8.jpg?1626139016";
        else if(id.equals("527378t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/6/86881c5f-df5e-4f50-b554-e4c49d5316f9.jpg?1625676073";
        else if(id.equals("532511t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/a/3/a378702b-d074-4402-b423-2ca8f44fce7c.jpg?1572370699";
        else if(id.equals("532519t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/2/62cafc0a-cd02-4265-aa1f-b8a6cb7cc8db.jpg?1592710150";
        else if(id.equals("532527t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/8/e/8e3b2942-d1a4-4d27-9d64-65712497ab2e.jpg?1561897504";
        else if(id.equals("532560t") || id.equals("532659t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/5/95483574-95b7-42a3-b700-616189163b0a.jpg?1598312392";
        else if(id.equals("531918t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/7/076f934b-a244-45f1-bcb3-7c5e882e9911.jpg?1594733476";
        else if(id.equals("532539t") || id.equals("531948t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/4/4/44a4ef4a-a026-424e-88ff-e2bb77aaf05d.jpg?1625974889";
        else if(id.equals("532482t") || id.equals("532493t") || id.equals("532491t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/e/4/e43a205e-43ea-4b3e-92ab-c2ee2172a50a.jpg?1572489150";
        else if(id.equals("532599t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/9/c/9c8fe0d7-5c40-45fe-b3d8-47852380845e.jpg?1562542392";
        else if(id.equals("531833t"))
            tokenurl = "https://i.pinimg.com/564x/04/dc/04/04dc041251acb96f97327d67e9c8fe23.jpg";
        else if(id.equals("532489t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/6/0/60842b1a-6ae7-4b3b-a23f-0d94a3d89884.jpg?1562639827";
        else if(id.equals("531921t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/2/3/2300635e-7771-4676-a5a5-29a9d8f49f1a.jpg?1604194799";
        else if(id.equals("531928t") || id.equals("531933t"))
            tokenurl = "https://c1.scryfall.com/file/scryfall-cards/large/front/0/b/0b08d210-01cb-46c5-9150-4dfb47f50ae7.jpg?1626139197";
        
        return tokenurl;
    }
    
    public boolean hasToken(String id){                
        if(id.equals("456378")      || id.equals("2912")    || id.equals("1514")    || id.equals("364")     || id.equals("69")      || id.equals("369012")  ||
                id.equals("417759") || id.equals("386476")  || id.equals("456371")  || id.equals("456360")  || id.equals("391958")  || id.equals("466959")  ||
                id.equals("466813") || id.equals("201176")  || id.equals("202483")  || id.equals("3546")    || id.equals("425949")  || id.equals("426027")  ||
                id.equals("425853") || id.equals("425846")  || id.equals("426036")  || id.equals("370387")  || id.equals("29955")   || id.equals("29989")   ||
                id.equals("19741")  || id.equals("19722")   || id.equals("19706")   || id.equals("24597")   || id.equals("24617")   || id.equals("24563")   || 
                id.equals("253539") || id.equals("277995")  || id.equals("265415")  || id.equals("289225")  || id.equals("289215")  || id.equals("253529")  || 
                id.equals("253641") || id.equals("270957")  || id.equals("401685")  || id.equals("89116")   || id.equals("5183")    || id.equals("5177")    ||
                id.equals("209289") || id.equals("198171")  || id.equals("10419")   || id.equals("470542")  || id.equals("29992")   || id.equals("666")     ||
                id.equals("2026")   || id.equals("45395")   || id.equals("442021")  || id.equals("423758")  || id.equals("426930")  || id.equals("998")     ||
                id.equals("446163") || id.equals("378411")  || id.equals("376457")  || id.equals("470749")  || id.equals("450641")  || id.equals("470623")  ||
                id.equals("470620") || id.equals("470754")  || id.equals("470750")  || id.equals("470739")  || id.equals("470708")  || id.equals("470581")  ||
                id.equals("470578") || id.equals("470571")  || id.equals("470552")  || id.equals("394490")  || id.equals("114921")  || id.equals("49775")   ||
                id.equals("473123") || id.equals("473160")  || id.equals("16743")   || id.equals("16741")   || id.equals("294493")  || id.equals("293253")  ||
                id.equals("293198") || id.equals("479634")  || id.equals("479702")  || id.equals("489837")  || id.equals("489861")  || id.equals("491359")  ||
                id.equals("294872") || id.equals("295110")  || id.equals("294842")  || id.equals("295067")  || id.equals("491767")  || id.equals("295386")  ||
                id.equals("295229") || id.equals("295387")  || id.equals("295206")  || id.equals("295706")  || id.equals("497549")  || id.equals("497666")  ||
                id.equals("503860") || id.equals("522280")  || id.equals("522111")  || id.equals("527288")  || id.equals("531927")  || id.equals("527295")  ||
                id.equals("111220") || id.equals("416829")  || id.equals("296545")  || id.equals("296694")  || id.equals("540473")  || id.equals("540464")  ||
                id.equals("540708") || id.equals("539395")  || id.equals("539417"))
            return false;
        return true;
    }
      
    public Document findTokenPage(String imageurl, String name, String set, String tokenstats, String color) throws Exception {
        Document doc;
        Elements outlinks;
        try {
            doc = Jsoup.connect(imageurl + "t" + set.toLowerCase()).get();
            if(doc != null) {
                outlinks = doc.select("body a");
                if(outlinks != null){
                    for (int k = 0; k < outlinks.size() && !interrupted; k++){
                        while(paused && !interrupted){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}
                        }
                        if(interrupted)
                            break;
                        String linktoken = outlinks.get(k).attributes().get("href");
                        if(linktoken != null && !linktoken.isEmpty()){
                            try {
                                Document tokendoc = Jsoup.connect(linktoken).get();
                                if(tokendoc == null)
                                    continue;
                                Elements stats = tokendoc.select("head meta");
                                if(stats != null) {
                                    for (int j = 0; j < stats.size() && !interrupted; j++){
                                        while(paused && !interrupted){
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {}
                                        }
                                        if(interrupted)
                                            break;
                                        if(stats.get(j).attributes().get("content").contains(tokenstats.replace("X/X", "*/*")) && 
                                                stats.get(j).attributes().get("content").toLowerCase().contains(name.toLowerCase())){
                                            return tokendoc;
                                        }
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
            }
        } catch (Exception e){}
        setTextArea("Warning: Token " + name + " has not been found between " + set + " tokens, i will search for it between any other set in " + imageurl + " (it may take a long time)", Color.blue, new Font("Arial", 1, 14));
        for (int i = 1; i < getSetCombo().getItemCount() && !interrupted; i++){
            while(paused && !interrupted){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
            if(interrupted)
                break;
            String currentSet = getSetCombo().getItemAt(i).toString().toLowerCase().split(" - ")[0];
            if(!currentSet.equalsIgnoreCase(set)){
                try {
                    doc = Jsoup.connect(imageurl + "t" + currentSet).get();
                    if(doc == null)
                        continue;
                    outlinks = doc.select("body a");
                    if(outlinks != null) {
                        for (int k = 0; k < outlinks.size() && !interrupted; k++){
                            while(paused && !interrupted){
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {}
                            }
                            if(interrupted)
                                break;
                            String linktoken = outlinks.get(k).attributes().get("href");
                            try {
                                Document tokendoc = Jsoup.connect(linktoken).get();
                                if(tokendoc == null)
                                    continue;
                                Elements stats = tokendoc.select("head meta");
                                if(stats != null) {
                                    for (int j = 0; j < stats.size() && !interrupted; j++){
                                        while(paused && !interrupted){
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {}
                                        }
                                        if(interrupted)
                                            break;
                                        if(stats.get(j).attributes().get("content").contains(tokenstats.replace("X/X", "*/*")) && 
                                                stats.get(j).attributes().get("content").toLowerCase().contains(name.toLowerCase())){
                                            setTextArea("Token " + name + " has been found between " + currentSet.toUpperCase() + " tokens, i will use this one", Color.black, new Font("Arial", 1, 14));
                                            return tokendoc;
                                        }
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                } catch (Exception e) {}
            }
        }
        setTextArea("Error: Token " + name + " has not been found in any set of " + imageurl, Color.red, new Font("Arial", 1, 14));
        throw new Exception();
    }
  
    volatile boolean loaded = false;
    
    Runnable Downloader = new Runnable() {
        @Override
        public void run() {
            try {
                if (SelectedSets.size() < 1 || WagicPath.isEmpty() || WagicPath.equalsIgnoreCase("Wrong installation path, sets folder not found...")){
                    setTextArea("Check all the parameters...", Color.red, new Font("Arial", 1, 14));
                    return;
                }

                String baseurl = "https://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=";
                String imageurl = "https://scryfall.com/sets/";
                String resPath = WagicPath + File.separator + "Res" + File.separator;
                String setsPath = WagicPath + File.separator + "Res" + File.separator + "sets" + File.separator;
                String destinationPath = WagicPath + File.separator + "User" + File.separator + "sets" + File.separator;

                Integer ImgX = 0;
                Integer ImgY = 0;
                Integer ThumbX = 0;
                Integer ThumbY = 0;
                Integer Border = 0;
                Integer BorderThumb = 0;
                
                if (targetres.equals("High")) {
                    ImgX = 672;
                    ImgY = 936;
                    ThumbX = 124;
                    ThumbY = 176;
                } else if (targetres.equals("Medium")) {
                    ImgX = 488;
                    ImgY = 680;
                    ThumbX = 90;
                    ThumbY = 128;
                } else if (targetres.equals("Low")) {
                    ImgX = 244;
                    ImgY = 340;
                    ThumbX = 45;
                    ThumbY = 64;
                } else if (targetres.equals("Tiny")) {
                    ImgX = 180;
                    ImgY = 255;
                    ThumbX = 45;
                    ThumbY = 64;
                }
               
                setTextArea("Download will start with these parameters:", Color.black, new Font("Arial", 1, 14));
                if(SelectedSets.size() == 1 && SelectedSets.get(0).equalsIgnoreCase("*.*"))
                    setTextArea("Number of sets to download: " + (getSetCombo().getItemCount() - 1), Color.black, new Font("Arial", 1, 14));
                else
                    setTextArea("Number of sets to download: " + SelectedSets.size(), Color.black, new Font("Arial", 1, 14));
                setTextArea("Target Resolution (cards): " + ImgX + "x" + ImgY, Color.black, new Font("Arial", 1, 14));
                setTextArea("Target Resolution (thumbnails): " + ThumbX + "x" + ThumbY, Color.black, new Font("Arial", 1, 14));
                setTextArea("Resources Dir: " + resPath, Color.black, new Font("Arial", 1, 14));
                setTextArea("User sets Dir: " + destinationPath, Color.black, new Font("Arial", 1, 14));
                setTextArea("\nDownloading Database...", Color.blue, new Font("Arial", 1, 14));
                
                long millis = System.currentTimeMillis();
                loaded = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        loadDatabase();
                    }
                }).start();
                
                while(!loaded && (System.currentTimeMillis() - millis < 30000)){
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                }
                
                if(!loaded)
                    setTextArea("Database could not be downloaded, i will use the slow image download method...\n", Color.red, new Font("Arial", 1, 14));
                else
                    setTextArea("Database successfully downloaded, i will use the fast image download method...\n", Color.blue, new Font("Arial", 1, 14));
                
                File resFolder = new File(WagicPath + File.separator + "Res" + File.separator);
                File [] listOfFile = resFolder.listFiles();
                File[] listOfSet = null;
                
                if(listOfFile != null && listOfFile.length > 1) {
                    File setsFolder = new File(setsPath);
                    listOfSet = setsFolder.listFiles();
                }

                for (int f = 0; f < SelectedSets.size() && !interrupted; f++) {
                    while(paused && !interrupted){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {}
                    }
                    if(interrupted)
                        break;
                    
                    String currentSet = SelectedSets.get(f);
                    Map<String, String> mappa = new HashMap<>();
                    String cardsfilepath = "";
                    boolean todelete = false;
                    if(listOfFile != null && listOfFile.length > 1) {
                        File folder = new File(setsPath + currentSet + File.separator);
                        cardsfilepath = folder.getAbsolutePath() + File.separator + "_cards.dat";
                    } else {
                        ZipFile zipFile = null;
                        InputStream stream;
                        java.nio.file.Path filePath;
                        try {
                            zipFile = new ZipFile(WagicPath + File.separator + "Res" + File.separator + listOfFile[0].getName());
                            Enumeration<? extends ZipEntry> e = zipFile.entries();
                            while (e.hasMoreElements()) {
                                ZipEntry entry = e.nextElement();
                                String entryName = entry.getName();
                                if(entryName != null && entryName.contains("sets/")){
                                    if(entryName.contains("_cards.dat")){
                                        String[] names = entryName.split("/");
                                        if(currentSet.equalsIgnoreCase(names[1])){
                                            stream = zipFile.getInputStream(entry);
                                            byte[] buffer = new byte[1];
                                            java.nio.file.Path outDir = Paths.get(WagicPath + File.separator + "Res" + File.separator);
                                            filePath = outDir.resolve("_cards.dat");
                                            try {
                                                FileOutputStream fos = new FileOutputStream(filePath.toFile());
                                                BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                                                int len;
                                                while ((len = stream.read(buffer)) != -1) {
                                                    bos.write(buffer, 0, len);
                                                }
                                                fos.close();
                                                bos.close();
                                                cardsfilepath = filePath.toString();
                                                todelete = true;
                                            } catch (Exception ex) {
                                                setTextArea("Error extracting Res zip file: " + ex, Color.red, new Font("Arial", 1, 14));
                                            }
                                            break;
                                        }		
                                    }
                                }
                            }	    
                        } catch (Exception ioe){
                            setTextArea("Error opening Res zip file: " + ioe, Color.red, new Font("Arial", 1, 14));
                        } finally {
                            try {
                                if (zipFile != null) {
                                    zipFile.close();
                                }
                            } catch (Exception ioe) {
                                setTextArea("Error while closing Res zip file: " + ioe, Color.red, new Font("Arial", 1, 14));
                            }
                        }
                    }
                    setTextArea("Currently downloading images for: " + currentSet, Color.black, new Font("Arial", 1, 14));
                    getProgressBar().setValue(0);
                    String lines = readLineByLineJava8(cardsfilepath);
                    if(todelete){
                        File del = new File(cardsfilepath);
                        del.delete();
                    }
                    int totalcards;
                    String findStr = "total=";
                    int lastIndex = lines.indexOf(findStr);
                    String totals = lines.substring(lastIndex, lines.indexOf("\n", lastIndex));
                    totalcards = Integer.parseInt(totals.split("=")[1]);
                    
                    if(borderless){
                        findStr = "year=";
                        lastIndex = lines.indexOf(findStr);
                        String date = lines.substring(lastIndex, lines.indexOf("\n", lastIndex));
                        int year = Integer.parseInt(date.split("=")[1].split("-")[0]);
                        int month = 1;
                        int day = 1;
                        if(date.split("=")[1].split("-").length > 1) {
                            month = Integer.parseInt(date.split("=")[1].split("-")[1]);
                            if(date.split("=")[1].split("-").length > 2) {
                                day = Integer.parseInt(date.split("=")[1].split("-")[2]);
                            }
                        }
                        if(year > 2014 || (year == 2014 && month > 6) || (year == 2014 && month == 6 && day > 15)){
                            Border = (int)Math.round(ImgX*0.02);
                            BorderThumb = (int)Math.round(ThumbX*0.02);
                        } else {
                            Border = (int)Math.round(ImgX*0.04);
                            BorderThumb = (int)Math.round(ThumbX*0.04);
                        }
                    } 
                    
                    while(lines.contains("[card]")) {
                        findStr = "[card]";
                        lastIndex = lines.indexOf(findStr);
                        String id = null;
                        String rarity = null;
                        String primitive = null;
                        boolean negativeId = false;
                        int a = lines.indexOf("primitive=",lastIndex);
                        if (a > 0){
                            if(lines.substring(a, lines.indexOf("\n", a)).split("=").length > 1)
                                primitive = lines.substring(a, lines.indexOf("\n", a)).split("=")[1];
			}
                        int b = lines.indexOf("id=", lastIndex);
                        if (b > 0){
                            if(lines.substring(b, lines.indexOf("\n", b)).contains("id=-"))
                                negativeId = true;
                            if(lines.substring(b, lines.indexOf("\n", b)).replace("-", "").split("=").length > 1)
                                id = lines.substring(b, lines.indexOf("\n", b)).replace("-", "").split("=")[1];
			}
                        int d = lines.indexOf("rarity=",lastIndex);
                        if(d > 0){
                           if(lines.substring(d, lines.indexOf("\n",d)).split("=").length > 1)
                               rarity = lines.substring(d, lines.indexOf("\n",d)).split("=")[1].toLowerCase();
                        }
                        if(rarity == null || !rarity.equals("t") || currentSet.equals("DKA") || currentSet.equals("EMN") || 
                                currentSet.equals("ISD") || currentSet.equals("ORI") || currentSet.equals("RIX") || currentSet.equals("V17") || 
                                currentSet.equals("UNH") || currentSet.equals("XLN") || currentSet.equals("SOI") || currentSet.equals("SOK") ||
                                currentSet.equals("BOK") || currentSet.equals("CHK") || currentSet.equals("ZNR") || currentSet.equals("KHM") ||
                                currentSet.equals("STX") || currentSet.equals("MID") || currentSet.equals("CC2"))
                            rarity = "";
                        if(id != null && !rarity.equals("t") && (negativeId || id.equals("209162") || id.equals("209163") || id.equals("401721") || 
                                id.equals("401722") || id.equals("999902")))
                            rarity = "t";
                        if(id != null && (id.equals("1750411") || id.equals("5176911") || id.equals("44680711") || id.equals("29530711") || 
                                id.equals("45108910") || id.equals("530447") || id.equals("530448") || id.equals("530449") || id.equals("296817") || 
                                id.equals("296818") || id.equals("29339510") || id.equals("1749810") || id.equals("5197410") || id.equals("5249510") || 
                                id.equals("5247310") || id.equals("5213710") || id.equals("5253010") || id.equals("5270410")))
                            rarity = "";
                        int c = lines.indexOf("[/card]",lastIndex);
                        if(c > 0)
                            lines = lines.substring(c + 8);
                        if (primitive != null && id != null && !id.equalsIgnoreCase("null")){
                            mappa.put(id + rarity, primitive);
                            if(id.equals("503837"))
                                 mappa.put("503837t", "Koma's Coil");
                            if(id.equals("503841"))
                                 mappa.put("503841t", "Shard");
                            if(id.equals("513652"))
                                 mappa.put("513652t", "Pest");
                            if(id.equals("513638"))
                                 mappa.put("513638t", "Pest");
                            if(id.equals("513543"))
                                 mappa.put("513543t", "Pest");
                            if(id.equals("513634"))
                                 mappa.put("513634t", "Fractal");
                            if(id.equals("530447"))
                                 mappa.put("530447t", "Skeleton");
                            if(id.equals("530448"))
                                 mappa.put("530448t", "Goblin");
                            if(id.equals("491633"))
                                 mappa.put("491633t", "Angel");
                            if(id.equals("114921")){
                                mappa.put("11492111t", "Citizen");
                                mappa.put("11492112t", "Camarid");
                                mappa.put("11492113t", "Thrull");
                                mappa.put("11492114t", "Goblin");
                                mappa.put("11492115t", "Saproling");
                            }
                        }
                    }

                    setTextArea("Total cards to download: " + totalcards, Color.black, new Font("Arial", 1, 14));
                    getProgressBar().setMaximum(totalcards);
                    File imgPath = new File(destinationPath + currentSet + File.separator);
                    if (!imgPath.exists()) {
                        setTextArea("Creating directory: " + imgPath.getName(), Color.black, new Font("Arial", 1, 14));
                        boolean result = false;
                        try {
                            imgPath.mkdir();
                            result = true;
                        } catch (Exception se) {
                            setTextArea("Error: " + imgPath + " not created", Color.red, new Font("Arial", 1, 14));
                            throw se;
                        }
                        if (result) {
                            setTextArea(imgPath + " created", Color.black, new Font("Arial", 1, 14));
                        }
                    }

                    File thumbPath = new File(destinationPath + currentSet + File.separator + "thumbnails" + File.separator);
                    if (!thumbPath.exists()) {
                        setTextArea("Creating directory: " + thumbPath.getName(), Color.black, new Font("Arial", 1, 14));
                        boolean result = false;
                        try {
                            thumbPath.mkdir();
                            result = true;
                        } catch (Exception se) {
                            setTextArea("Error: " + thumbPath + " not created", Color.red, new Font("Arial", 1, 14));
                            throw se;
                        }
                        if (result) {
                            setTextArea(thumbPath + " created", Color.black, new Font("Arial", 1, 14));
                        }
                    }
                    
                    String scryset = currentSet;
                    if(scryset.equalsIgnoreCase("MRQ"))
                        scryset = "MMQ";
                    else if(scryset.equalsIgnoreCase("AVN"))
                        scryset = "DDH";
                    else if(scryset.equalsIgnoreCase("BVC"))
                        scryset = "DDQ";
                    else if(scryset.equalsIgnoreCase("CFX"))
                        scryset = "CON";
                    else if(scryset.equalsIgnoreCase("DM"))
                        scryset = "DKM";
                    else if(scryset.equalsIgnoreCase("EVK"))
                        scryset = "DDO";
                    else if(scryset.equalsIgnoreCase("EVT"))
                        scryset = "DDF";
                    else if(scryset.equalsIgnoreCase("FVD"))
                        scryset = "DRB";
                    else if(scryset.equalsIgnoreCase("FVE"))
                        scryset = "V09";
                    else if(scryset.equalsIgnoreCase("FVL"))
                        scryset = "V11";
                    else if(scryset.equalsIgnoreCase("FVR"))
                        scryset = "V10";
                    else if(scryset.equalsIgnoreCase("HVM"))
                        scryset = "DDL";
                    else if(scryset.equalsIgnoreCase("IVG"))
                        scryset = "DDJ";
                    else if(scryset.equalsIgnoreCase("JVV"))
                        scryset = "DDM";
                    else if(scryset.equalsIgnoreCase("KVD"))
                        scryset = "DDG";
                    else if(scryset.equalsIgnoreCase("PDS"))
                        scryset = "H09";
                    else if(scryset.equalsIgnoreCase("PVC"))
                        scryset = "DDE";
                    else if(scryset.equalsIgnoreCase("RV"))
                        scryset = "3ED";
                    else if(scryset.equalsIgnoreCase("SVT"))
                        scryset = "DDK";
                    else if(scryset.equalsIgnoreCase("VVK"))
                        scryset = "DDI";
                    else if(scryset.equalsIgnoreCase("ZVE"))
                        scryset = "DDP";

                    for (int y = 0; y < mappa.size() && !interrupted; y++) {
                        while(paused && !interrupted){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}
                        }
                        if(interrupted)
                            break;
                        
                        String id = mappa.keySet().toArray()[y].toString();
                        getProgressBar().setStringPainted(true);
                        getProgressBar().setString("Downloading Card: " + mappa.get(id) + " (" + id + ".jpg)" + " - " + y + " of " + mappa.size() + "");
                        getProgressBar().setValue(y);
                        if(fastDownloadCard(currentSet, id, mappa.get(id), imgPath.getAbsolutePath(), thumbPath.getAbsolutePath(), ImgX, ImgY, ThumbX, ThumbY, Border, BorderThumb))
                            continue;
                        String specialcardurl = getSpecialCardUrl(id, currentSet);
                        if(!specialcardurl.isEmpty()){
                            if(cardlist != null){
                                cardlist.append(currentSet + ";" + id + ";" + specialcardurl + "\n");
                                cardlist.flush();
                            }
                            URL url = new URL(specialcardurl);
                            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
                            if(httpcon == null) {
                                setTextArea("Error: Problem fetching card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                break;
                            }
                            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
                            httpcon.setConnectTimeout(5000);
                            httpcon.setReadTimeout(5000);
                            httpcon.setAllowUserInteraction(false);
                            httpcon.setDoInput(true);
                            httpcon.setDoOutput(false);
                            InputStream in = null;
                            try{
                                in = new BufferedInputStream(httpcon.getInputStream());
                            }catch(Exception ex){
                                setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                try {
                                    in = new BufferedInputStream(httpcon.getInputStream());
                                } catch (Exception ex2) {
                                    setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                    try {
                                        in = new BufferedInputStream(httpcon.getInputStream());
                                    } catch (Exception ex3) {
                                        setTextArea("Error: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                        break;
                                    }
                                }
                            }
                            String cardimage = imgPath + File.separator + id + ".jpg";
                            String thumbcardimage = thumbPath + File.separator + id + ".jpg";
                            try{
                                ReadableByteChannel readableByteChannel = Channels.newChannel(httpcon.getInputStream());
                                FileOutputStream fileOutputStream = new FileOutputStream(cardimage);
                                FileChannel fileChannel = fileOutputStream.getChannel();
                                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                fileChannel.close();
                                fileOutputStream.close();

                                fileOutputStream = new FileOutputStream(thumbcardimage);
                                fileChannel = fileOutputStream.getChannel();
                                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                fileChannel.close();
                                fileOutputStream.close();
                                readableByteChannel.close();

                                Toolkit toolkit = Toolkit.getDefaultToolkit();
                                MediaTracker tracker = new MediaTracker(new Panel());
                                Image image = toolkit.getImage(cardimage);
                                tracker.addImage(image, 0);
                                try {
                                    tracker.waitForAll();
                                } catch (Exception e) { }

                                BufferedImage resizedImg = new BufferedImage(ImgX, ImgY, BufferedImage.TYPE_INT_RGB);
                                Graphics2D tGraphics2DReiszed = resizedImg.createGraphics(); //create a graphics object to paint to
                                if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                        currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                        currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                        currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                    tGraphics2DReiszed.setBackground(Color.WHITE);
                                    tGraphics2DReiszed.setPaint(Color.WHITE);
                                }else {
                                    tGraphics2DReiszed.setBackground(Color.BLACK);
                                    tGraphics2DReiszed.setPaint(Color.BLACK);
                                }
                                tGraphics2DReiszed.fillRect(0, 0, ImgX, ImgY);
                                tGraphics2DReiszed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                tGraphics2DReiszed.drawImage(image, 0, 0, ImgX, ImgY, null); //draw the image scaled
                                resizedImg = resizedImg.getSubimage(Border, Border, ImgX-2*Border, ImgY-2*Border);
                                ImageIO.write(resizedImg, "JPG", new File(cardimage)); //write the image to a file

                                BufferedImage tThumbImage = new BufferedImage(ThumbX, ThumbY, BufferedImage.TYPE_INT_RGB);
                                Graphics2D tGraphics2D = tThumbImage.createGraphics(); //create a graphics object to paint to
                                if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                        currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                        currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                        currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                    tGraphics2D.setBackground(Color.WHITE);
                                    tGraphics2D.setPaint(Color.WHITE);
                                }else {
                                    tGraphics2D.setBackground(Color.BLACK);
                                    tGraphics2D.setPaint(Color.BLACK);
                                }
                                tGraphics2D.fillRect(0, 0, ThumbX, ThumbY);
                                tGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                tGraphics2D.drawImage(image, 0, 0, ThumbX, ThumbY, null); //draw the image scaled
                                tThumbImage = tThumbImage.getSubimage(BorderThumb, BorderThumb, ThumbX-2*BorderThumb, ThumbY-2*BorderThumb);
                                ImageIO.write(tThumbImage, "JPG", new File(thumbcardimage)); //write the image to a file
                            } catch(Exception e) {
                                setTextArea("Error: Problem saving card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                break;
                            }
                            continue;
                        }
                        if(id.endsWith("t"))
                            continue;
                                                
                        Document doc = null;
                        String cardname = mappa.get(id);
                        Elements divs = new Elements();
                        int k;
                        if(scryset.equals("TD2") || scryset.equals("PRM") || scryset.equals("TD0") || scryset.equals("PZ1") || scryset.equals("PZ2")
                                || scryset.equals("PHPR") || scryset.equals("PGRU") || scryset.equals("PIDW") || scryset.equals("ANA") || scryset.equals("HTR") 
                                || scryset.equals("HTR17") || scryset.equals("PI13") || scryset.equals("PI14") || scryset.equals("PSAL") || scryset.equals("PS11")
                                || scryset.equals("PDTP") || scryset.equals("PDP10") || scryset.equals("PDP11") || scryset.equals("PDP12") || scryset.equals("PDP13")
                                || scryset.equals("PDP14") || scryset.equals("DPA") || scryset.equals("PMPS") || scryset.equals("PMPS06") || scryset.equals("PMPS07")
                                || scryset.equals("PMPS08") || scryset.equals("PMPS09") || scryset.equals("PMPS10") || scryset.equals("PMPS11") || scryset.equals("GN2")
                                || scryset.equals("PAL00") || scryset.equals("PAL01") || scryset.equals("PAL02") || scryset.equals("PAL03") || scryset.equals("PAL04")
                                || scryset.equals("PAL05") || scryset.equals("PAL06") || scryset.equals("PAL99") || scryset.equals("PARL") || scryset.equals("HA1")
                                || scryset.equals("SLD") || scryset.equals("MB1") || scryset.equals("HA2") || scryset.equals("HA3") || scryset.equals("SS3")
                                || scryset.equals("AKR") || scryset.equals("ANB") || scryset.equals("PLIST") || scryset.equals("KLR") || scryset.equals("CC1") 
                                || scryset.equals("ATH") || scryset.equals("HA4") || scryset.equals("TSR") || scryset.equals("HA5") || scryset.equals("H1R")
                                || scryset.equals("HTR18") || scryset.equals("HTR19") || scryset.equals("DKM") || scryset.equals("S00") || scryset.equals("XLN")
                                || scryset.equals("SOI") || scryset.equals("UST") || scryset.equals("PLG21") || scryset.equals("J21") || scryset.equals("CC2")
                                || scryset.equals("Q06")){
                            try {
                                doc = Jsoup.connect(imageurl + scryset.toLowerCase()).maxBodySize(0)
                                    .timeout(100000*5)
                                    .get();
                                Elements outlinks = doc.select("body a");
                                if(outlinks != null){
                                    for (int h = 0; h < outlinks.size(); h++){
                                        String linkcard = outlinks.get(h).attributes().get("href");
                                        if(linkcard == null)
                                            continue;
                                        String strtork[] = linkcard.toLowerCase().split("/");
                                        if(strtork.length <= 0)
                                            continue;
                                        String nametocmp = strtork[strtork.length - 1];
                                        if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                            try {
                                                doc = Jsoup.connect(linkcard).get();
                                                if(doc == null)
                                                    continue;
                                                Elements metadata = doc.select("head meta");
                                                if(metadata != null) {
                                                    for (int j = 0; j < metadata.size(); j++){
                                                        if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                            h = outlinks.size();
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {}
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                try {
                                    doc = Jsoup.connect(imageurl + scryset.toLowerCase()).maxBodySize(0)
                                        .timeout(100000*5)
                                        .get();
                                    Elements outlinks = doc.select("body a");
                                    if(outlinks != null){
                                        for (int h = 0; h < outlinks.size(); h++){
                                            String linkcard = outlinks.get(h).attributes().get("href");
                                            if(linkcard == null)
                                                continue;
                                            String strtork[] = linkcard.toLowerCase().split("/");
                                            if(strtork.length <= 0)
                                                continue;
                                            String nametocmp = strtork[strtork.length - 1];
                                            if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                                try {
                                                    doc = Jsoup.connect(linkcard).get();
                                                    if(doc == null)
                                                        continue;
                                                    Elements metadata = doc.select("head meta");
                                                    if(metadata != null) {
                                                        for (int j = 0; j < metadata.size(); j++){
                                                            if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                                h = outlinks.size();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                } catch (Exception ex) {}
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                    try {
                                        doc = Jsoup.connect(imageurl + scryset.toLowerCase()).maxBodySize(0)
                                            .timeout(100000*5)
                                            .get();
                                        Elements outlinks = doc.select("body a");
                                        if(outlinks != null){
                                            for (int h = 0; h < outlinks.size(); h++){
                                                String linkcard = outlinks.get(h).attributes().get("href");
                                                if(linkcard == null)
                                                    continue;
                                                String strtork[] = linkcard.toLowerCase().split("/");
                                                if(strtork.length <= 0)
                                                    continue;
                                                String nametocmp = strtork[strtork.length - 1];
                                                if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                                    try {
                                                        doc = Jsoup.connect(linkcard).get();
                                                        if(doc == null)
                                                            continue;
                                                        Elements metadata = doc.select("head meta");
                                                        if(metadata != null) {
                                                            for (int j = 0; j < metadata.size(); j++){
                                                                if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                                    h = outlinks.size();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } catch (Exception ex) {}
                                                }
                                            }
                                        }
                                    } catch (Exception e3) {
                                        setTextArea("Error: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                        continue;
                                    }
                                }
                            }
                        } else {
                            try{
                                doc = Jsoup.connect(baseurl + id).get();
                            } catch(Exception e) {
                                setTextArea("Warning: Problem reading card (" + mappa.get(id) + ") infos from: " + baseurl  + id + ", i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                try{
                                    doc = Jsoup.connect(baseurl + id).get();
                                } catch(Exception e2) {
                                    setTextArea("Warning: Problem reading card (" + mappa.get(id) + ") infos from: " + baseurl  + id + ", i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                    try{
                                        doc = Jsoup.connect(baseurl + id).get();
                                    } catch(Exception e3) {
                                        setTextArea("Error: Problem reading card (" + mappa.get(id) + ") infos from: " + baseurl  + id + ", i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                        continue;
                                    }
                                }
                            }
                            if(doc == null){
                                setTextArea("Error: Problem reading card (" + mappa.get(id) + ") infos from: " + baseurl  + id + ", i can't download it...", Color.red, new Font("Arial", 1, 14));
                                continue;
                            }
                            divs = doc.select("body div");
                            if(divs == null){
                                setTextArea("Error: Problem reading card (" + mappa.get(id) + ") infos from: " + baseurl  + id + ", i can't download it...", Color.red, new Font("Arial", 1, 14));
                                continue;
                            }
                        }
                        while(paused && !interrupted){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}
                        }
                        if(interrupted)
                            break;
                        
                        if(targetres.equals("High") && !scryset.equals("TD2") && !scryset.equals("PRM") && !scryset.equals("TD0") && !scryset.equals("PZ2")
                                && !scryset.equals("PHPR") && !scryset.equals("PGRU") && !scryset.equals("PGRU") && !scryset.equals("ANA") && !scryset.equals("HTR") 
                                && !scryset.equals("HTR17") && !scryset.equals("PI13") && !scryset.equals("PI14") && !scryset.equals("PSAL") && !scryset.equals("PS11")
                                && !scryset.equals("PDTP") && !scryset.equals("PDP10") && !scryset.equals("PDP11") && !scryset.equals("PDP12") && !scryset.equals("PDP13")
                                && !scryset.equals("PDP14") && !scryset.equals("DPA") && !scryset.equals("PMPS") && !scryset.equals("PMPS06") && !scryset.equals("PMPS07")
                                && !scryset.equals("PMPS08") && !scryset.equals("PMPS09") && !scryset.equals("PMPS10") && !scryset.equals("PMPS11") && !scryset.equals("GN2")
                                && !scryset.equals("PAL00") && !scryset.equals("PAL01") && !scryset.equals("PAL02") && !scryset.equals("PAL03") && !scryset.equals("PAL04")
                                && !scryset.equals("PAL05") && !scryset.equals("PAL06") && !scryset.equals("PAL99") && !scryset.equals("PARL") && !scryset.equals("HA1")
                                && !scryset.equals("SLD") && !scryset.equals("MB1") && !scryset.equals("HA2") && !scryset.equals("HA3") && !scryset.equals("SS3")
                                && !scryset.equals("AKR") && !scryset.equals("ANB") && !scryset.equals("PLIST") && !scryset.equals("KLR") && !scryset.equals("CC1")
                                && !scryset.equals("ATH") && !scryset.equals("HA4") && !scryset.equals("TSR") && !scryset.equals("HA5") && !scryset.equals("H1R")
                                && !scryset.equals("HTR18") && !scryset.equals("HTR19") && !scryset.equals("DKM") && !scryset.equals("S00") && !scryset.equals("XLN")
                                && !scryset.equals("SOI") && !scryset.equals("UST") && !scryset.equals("PLG21") && !scryset.equals("J21") && !scryset.equals("CC2")
                                && !scryset.equals("Q06")){
                            try {
                                doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                Elements outlinks = doc.select("body a");
                                if(outlinks != null){
                                    for (int h = 0; h < outlinks.size(); h++){
                                        String linkcard = outlinks.get(h).attributes().get("href");
                                        if(linkcard == null)
                                            continue;
                                        String strtork[] = linkcard.toLowerCase().split("/");
                                        if(strtork.length <= 0)
                                            continue;
                                        String nametocmp = strtork[strtork.length - 1];
                                        if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                            try {
                                                doc = Jsoup.connect(linkcard).get();
                                                if(doc == null)
                                                    continue;
                                                Elements metadata = doc.select("head meta");
                                                if(metadata != null) {
                                                    for (int j = 0; j < metadata.size(); j++){
                                                        if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                            h = outlinks.size();
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {}
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                try {
                                    doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                    Elements outlinks = doc.select("body a");
                                    if(outlinks != null){
                                        for (int h = 0; h < outlinks.size(); h++){
                                            String linkcard = outlinks.get(h).attributes().get("href");
                                            if(linkcard == null)
                                                continue;
                                            String strtork[] = linkcard.toLowerCase().split("/");
                                            if(strtork.length <= 0)
                                                continue;
                                            String nametocmp = strtork[strtork.length - 1];
                                            if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                                try {
                                                    doc = Jsoup.connect(linkcard).get();
                                                    if(doc == null)
                                                        continue;
                                                    Elements metadata = doc.select("head meta");
                                                    if(metadata != null) {
                                                        for (int j = 0; j < metadata.size(); j++){
                                                            if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                                h = outlinks.size();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                } catch (Exception ex) {}
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                    try {
                                        doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                        Elements outlinks = doc.select("body a");
                                        if(outlinks != null){
                                            for (int h = 0; h < outlinks.size(); h++){
                                                String linkcard = outlinks.get(h).attributes().get("href");
                                                if(linkcard == null)
                                                    continue;
                                                String strtork[] = linkcard.toLowerCase().split("/");
                                                if(strtork.length <= 0)
                                                    continue;
                                                String nametocmp = strtork[strtork.length - 1];
                                                if(nametocmp.equals(cardname.toLowerCase().replace(" ", "-"))){
                                                    try {
                                                        doc = Jsoup.connect(linkcard).get();
                                                        if(doc == null)
                                                            continue;
                                                        Elements metadata = doc.select("head meta");
                                                        if(metadata != null) {
                                                            for (int j = 0; j < metadata.size(); j++){
                                                                if(metadata.get(j).attributes().get("content").toLowerCase().equals(cardname.toLowerCase())){
                                                                    h = outlinks.size();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } catch (Exception ex) {}
                                                }
                                            }
                                        }
                                    } catch (Exception e3) {
                                        setTextArea("Error: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                        continue;
                                    }
                                }
                            }
                        } else if(!scryset.equals("TD2") && !scryset.equals("PRM") && !scryset.equals("TD0") && !scryset.equals("PZ1") && !scryset.equals("PZ2")
                                && !scryset.equals("PHPR") && !scryset.equals("PGRU") && !scryset.equals("PGRU") && !scryset.equals("ANA") && !scryset.equals("HTR") 
                                && !scryset.equals("HTR17") && !scryset.equals("PI13") && !scryset.equals("PI14") && !scryset.equals("PSAL") && !scryset.equals("PS11")
                                && !scryset.equals("PDTP") && !scryset.equals("PDP10") && !scryset.equals("PDP11") && !scryset.equals("PDP12") && !scryset.equals("PDP13")
                                && !scryset.equals("PDP14") && !scryset.equals("DPA") && !scryset.equals("PMPS") && !scryset.equals("PMPS06") && !scryset.equals("PMPS07")
                                && !scryset.equals("PMPS08") && !scryset.equals("PMPS09") && !scryset.equals("PMPS10") && !scryset.equals("PMPS11") && !scryset.equals("GN2")
                                && !scryset.equals("PAL00") && !scryset.equals("PAL01") && !scryset.equals("PAL02") && !scryset.equals("PAL03") && !scryset.equals("PAL04")
                                && !scryset.equals("PAL05") && !scryset.equals("PAL06") && !scryset.equals("PAL99") && !scryset.equals("PARL") && !scryset.equals("HA1")
                                && !scryset.equals("SLD") && !scryset.equals("MB1") && !scryset.equals("HA2") && !scryset.equals("HA3") && !scryset.equals("SS3")
                                && !scryset.equals("AKR") && !scryset.equals("ANB") && !scryset.equals("PLIST") && !scryset.equals("KLR") && !scryset.equals("CC1")
                                && !scryset.equals("ATH") && !scryset.equals("HA4") && !scryset.equals("TSR") && !scryset.equals("HA5") && !scryset.equals("H1R")
                                && !scryset.equals("HTR18") && !scryset.equals("HTR19") && !scryset.equals("DKM") && !scryset.equals("S00") && !scryset.equals("XLN")
                                && !scryset.equals("SOI") && !scryset.equals("UST") && !scryset.equals("PLG21") && !scryset.equals("J21") && !scryset.equals("CC2") 
                                && !scryset.equals("Q06")){
                            try {
                                doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                            } catch (Exception e) {
                                setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                try {
                                    doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                } catch (Exception e2) {
                                    setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                    try {
                                        doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                    } catch (Exception e3) {
                                        setTextArea("Error: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                        continue;
                                    }
                                }
                            }
                        }
                        
                        if(doc == null){
                            setTextArea("Error: Problem fetching card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                            continue;
                        }
                        
                        Elements imgs = doc.select("body img");
                        if(imgs == null){
                            setTextArea("Error: Problem fetching card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                            continue;
                        }
                        
                        for (int i = 0; i < imgs.size() && !interrupted; i++) {
                            while(paused && !interrupted){
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {}
                            }
                            if(interrupted)
                                break;
                            String title = imgs.get(i).attributes().get("alt");
                            if(title.isEmpty())
                                title = imgs.get(i).attributes().get("title");
                            else
                                title = title.split("from")[0];
                            if (title.replace("(" + scryset + ")","").replace("(NEM)","").trim().toLowerCase().equals(cardname.toLowerCase())) {
                                String CardImage = imgs.get(i).attributes().get("src");
                                if (CardImage.isEmpty())
                                    CardImage = imgs.get(i).attributes().get("data-src");
                                CardImage = CardImage.replace("/normal/", "/large/");
                                                         
                                if(cardlist != null){
                                    cardlist.append(currentSet + ";" + id + ";" + CardImage + "\n");
                                    cardlist.flush();
                                }
                                
                                URL url = new URL(CardImage);
                                HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
                                if(httpcon == null) {
                                    setTextArea("Error: Problem fetching card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                    break;
                                }
                                httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
                                httpcon.setConnectTimeout(5000);
                                httpcon.setReadTimeout(5000);
                                httpcon.setAllowUserInteraction(false);
                                httpcon.setDoInput(true);
                                httpcon.setDoOutput(false);
                                InputStream in = null;
                                try{
                                    in = new BufferedInputStream(httpcon.getInputStream());
                                }catch(Exception ex){
                                    setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                    try {
                                        in = new BufferedInputStream(httpcon.getInputStream());
                                    } catch (Exception ex2) {
                                        setTextArea("Warning: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                        try {
                                            in = new BufferedInputStream(httpcon.getInputStream());
                                        } catch (Exception ex3) {
                                            setTextArea("Error: Problem downloading card: " + mappa.get(id) + " (" + id + ".jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                            break;
                                        }
                                    }
                                }                               

                                String cardimage = imgPath + File.separator + id + ".jpg";
                                String thumbcardimage = thumbPath + File.separator + id + ".jpg";
                                
                                try {
                                    ReadableByteChannel readableByteChannel = Channels.newChannel(httpcon.getInputStream());
                                    FileOutputStream fileOutputStream = new FileOutputStream(cardimage);
                                    FileChannel fileChannel = fileOutputStream.getChannel();
                                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                    fileChannel.close();
                                    fileOutputStream.close();

                                    fileOutputStream = new FileOutputStream(thumbcardimage);
                                    fileChannel = fileOutputStream.getChannel();
                                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                    fileChannel.close();
                                    fileOutputStream.close();
                                    readableByteChannel.close();

                                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                                    MediaTracker tracker = new MediaTracker(new Panel());
                                    Image image = toolkit.getImage(cardimage);
                                    tracker.addImage(image, 0);
                                    try {
                                        tracker.waitForAll();
                                    } catch (Exception e) { }

                                    BufferedImage resizedImg = new BufferedImage(ImgX, ImgY, BufferedImage.TYPE_INT_RGB);
                                    Graphics2D tGraphics2DReiszed = resizedImg.createGraphics(); //create a graphics object to paint to
                                    if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                            currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                            currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                            currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                        tGraphics2DReiszed.setBackground(Color.WHITE);
                                        tGraphics2DReiszed.setPaint(Color.WHITE);
                                    }else {
                                        tGraphics2DReiszed.setBackground(Color.BLACK);
                                        tGraphics2DReiszed.setPaint(Color.BLACK);
                                    }
                                    tGraphics2DReiszed.fillRect(0, 0, ImgX, ImgY);
                                    tGraphics2DReiszed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    tGraphics2DReiszed.drawImage(image, 0, 0, ImgX, ImgY, null); //draw the image scaled
                                    resizedImg = resizedImg.getSubimage(Border, Border, ImgX-2*Border, ImgY-2*Border);
                                    ImageIO.write(resizedImg, "JPG", new File(cardimage)); //write the image to a file

                                    BufferedImage tThumbImage = new BufferedImage(ThumbX, ThumbY, BufferedImage.TYPE_INT_RGB);
                                    Graphics2D tGraphics2D = tThumbImage.createGraphics(); //create a graphics object to paint to
                                    if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                            currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                            currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                            currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                        tGraphics2D.setBackground(Color.WHITE);
                                        tGraphics2D.setPaint(Color.WHITE);
                                    }else {
                                        tGraphics2D.setBackground(Color.BLACK);
                                        tGraphics2D.setPaint(Color.BLACK);
                                    }
                                    tGraphics2D.fillRect(0, 0, ThumbX, ThumbY);
                                    tGraphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    tGraphics2D.drawImage(image, 0, 0, ThumbX, ThumbY, null); //draw the image scaled
                                    tThumbImage = tThumbImage.getSubimage(BorderThumb, BorderThumb, ThumbX-2*BorderThumb, ThumbY-2*BorderThumb);
                                    ImageIO.write(tThumbImage, "JPG", new File(thumbcardimage)); //write the image to a file
                                } catch (Exception e){
                                    setTextArea("Error: Problem saving card: " + mappa.get(id) + " (" + id + ".jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                    break;
                                }
                                String text = "";
                                if(scryset.equals("PRM") || scryset.equals("TD0") || scryset.equals("PZ1") || scryset.equals("PZ2") || scryset.equals("PHPR")
                                        || scryset.equals("PGRU") || scryset.equals("PGRU") || scryset.equals("ANA") || scryset.equals("HTR") || scryset.equals("HTR17")
                                        || scryset.equals("PI13") || scryset.equals("PI14") || scryset.equals("PSAL") || scryset.equals("PS11") || scryset.equals("PDTP") 
                                        || scryset.equals("PDP10") || scryset.equals("PDP11") || scryset.equals("PDP12") || scryset.equals("PDP13") || scryset.equals("PDP14") 
                                        || scryset.equals("DPA") || scryset.equals("PMPS") || scryset.equals("PMPS06") || scryset.equals("PMPS07") || scryset.equals("PMPS08") 
                                        || scryset.equals("PMPS09") || scryset.equals("PMPS10") || scryset.equals("PMPS11") || scryset.equals("GN2") || scryset.equals("PAL00") 
                                        || scryset.equals("PAL01") || scryset.equals("PAL02") || scryset.equals("PAL03") || scryset.equals("PAL04") || scryset.equals("PAL05") 
                                        || scryset.equals("PAL06") || scryset.equals("PAL99") || scryset.equals("PARL") || scryset.equals("HA1") || scryset.equals("SLD")
                                        || scryset.equals("MB1") || scryset.equals("HA2") || scryset.equals("HA3") || scryset.equals("SS3") || scryset.equals("AKR") 
                                        || scryset.equals("ANB") || scryset.equals("PLIST") || scryset.equals("KLR") || scryset.equals("CC1") || scryset.equals("ATH")
                                        || scryset.equals("HA4") || scryset.equals("TSR") || scryset.equals("HA5") || scryset.equals("H1R") || scryset.equals("HTR18")
                                        || scryset.equals("HTR19") || scryset.equals("DKM") || scryset.equals("S00") || scryset.equals("XLN") || scryset.equals("SOI")
                                        || scryset.equals("UST") || scryset.equals("PLG21") || scryset.equals("J21") || scryset.equals("CC2") || scryset.equals("Q06")){
                                    Elements metadata = doc.select("head meta");
                                    if(metadata != null) {
                                        for (int j = 0; j < metadata.size(); j++){
                                            if(metadata.get(j).attributes().get("property").equals("og:description")){
                                                if(metadata.get(j).attributes().get("content").split("•").length > 3){
                                                    text = metadata.get(j).attributes().get("content").split("•")[3].trim();
                                                    if (text.contains("(" + scryset + ")"))
                                                        text = metadata.get(j).attributes().get("content").split("•")[2].trim();
                                                    if (text.contains("Illustrated by"))
                                                        text = metadata.get(j).attributes().get("content").split("•")[1].trim();
                                                    text = text.replace("&#39;", "'");
                                                    break;
                                                } else
                                                    break;
                                            }
                                        }
                                    }
                                } else { 
                                    for (k = 0; k < divs.size(); k++)
                                        if (divs.get(k).childNodes().size() > 0 && divs.get(k).childNode(0).toString().toLowerCase().contains("card text"))
                                            break;
                                    if (k < divs.size()) {
                                        Element tex = divs.get(k + 1);
                                        for (int z = 0; z < divs.get(k + 1).childNodes().size(); z++) {
                                            for (int u = 0; u < divs.get(k + 1).childNode(z).childNodes().size(); u++) {
                                                if (divs.get(k + 1).childNode(z).childNode(u).childNodes().size() > 1) {
                                                    for (int w = 0; w < divs.get(k + 1).childNode(z).childNode(u).childNodes().size(); w++) {
                                                        if (divs.get(k + 1).childNode(z).childNode(u).childNode(w).hasAttr("alt")) {
                                                            String newtext = divs.get(k + 1).childNode(z).childNode(u).childNode(w).attributes().get("alt").trim();
                                                            newtext = newtext.replace("Green", "{G}");
                                                            newtext = newtext.replace("White", "{W}");
                                                            newtext = newtext.replace("Black", "{B}");
                                                            newtext = newtext.replace("Blue", "{U}");
                                                            newtext = newtext.replace("Red", "{R}");
                                                            newtext = newtext.replace("Tap", "{T}");
                                                            text = text + newtext;
                                                        } else
                                                            text = text + " " + divs.get(k + 1).childNode(z).childNode(u).childNode(w).toString().replace("\r\n", "").trim() + " ";
                                                        text = text.replace("} .", "}.");
                                                        text = text.replace("} :", "}:");
                                                        text = text.replace("} ,", "},");
                                                    }
                                                } else {
                                                    if (divs.get(k + 1).childNode(z).childNode(u).hasAttr("alt")) {
                                                        String newtext = divs.get(k + 1).childNode(z).childNode(u).attributes().get("alt").trim();
                                                        newtext = newtext.replace("Green", "{G}");
                                                        newtext = newtext.replace("White", "{W}");
                                                        newtext = newtext.replace("Black", "{B}");
                                                        newtext = newtext.replace("Blue", "{U}");
                                                        newtext = newtext.replace("Red", "{R}");
                                                        newtext = newtext.replace("Tap", "{T}");
                                                        text = text + newtext;
                                                    } else
                                                        text = text + " " + divs.get(k + 1).childNode(z).childNode(u).toString().replace("\r\n", "").trim() + " ";
                                                    text = text.replace("} .", "}.");
                                                    text = text.replace("} :", "}:");
                                                    text = text.replace("} ,", "},");
                                                }
                                                if (z > 0 && z < divs.get(k + 1).childNodes().size() - 1)
                                                    text = text + " -- ";
                                                text = text.replace("<i>", "");
                                                text = text.replace("</i>", "");
                                                text = text.replace("<b>", "");
                                                text = text.replace("</b>", "");
                                                text = text.replace(" -- (", " (");
                                                text = text.replace("  ", " ");
                                            }
                                        }
                                    }
                                }
                                if (hasToken(id) && ((text.trim().toLowerCase().contains("create") && text.trim().toLowerCase().contains("creature token")) || 
                                        (text.trim().toLowerCase().contains("put") && text.trim().toLowerCase().contains("token")))) {
                                    setTextArea("The card: " + mappa.get(id) + " (" + id + ".jpg) can create a token, i will try to download that image too as " + id + "t.jpg", Color.black, new Font("Arial", 1, 14));
                                    boolean tokenfound;
                                    String arrays[] = text.trim().split(" ");
                                    String nametoken = "";
                                    String nametocheck = "";
                                    String tokenstats = "";
                                    String color = "";
                                    String color1 = "";
                                    String color2 = "";
                                    for (int l = 1; l < arrays.length - 1; l++) {
                                        if (arrays[l].equalsIgnoreCase("creature") && arrays[l + 1].toLowerCase().contains("token")) {
                                            nametoken = arrays[l - 1];
                                            if(l - 3 > 0){
                                                tokenstats = arrays[l - 3];
                                                color1 = arrays[l - 2];
                                            }
                                            if(!tokenstats.contains("/")){
                                                if(l - 4 > 0){
                                                    tokenstats = arrays[l - 4];
                                                    color1 = arrays[l - 3];
                                                }
                                            }
                                            if(!tokenstats.contains("/")){
                                                if(l - 5 > 0){
                                                    tokenstats = arrays[l - 5];
                                                    color1 = arrays[l - 4];
                                                    color2 = arrays[l - 2];
                                                }
                                            }
                                            if(!tokenstats.contains("/")){
                                                if(l - 6 > 0){
                                                    tokenstats = arrays[l - 6];
                                                    color1 = arrays[l - 5];
                                                    color2 = arrays[l - 3];
                                                }
                                            }
                                            if(!tokenstats.contains("/")){
                                                if(l - 7 > 0){
                                                    tokenstats = arrays[l - 7];
                                                    color1 = arrays[l - 6];
                                                    color2 = arrays[l - 4];
                                                }
                                            }
                                            if(nametoken.equalsIgnoreCase("artifact")){
                                                if(l - 2 > 0)
                                                    nametoken = arrays[l - 2];
                                                if(l - 4 > 0){
                                                    tokenstats = arrays[l - 4];
                                                    color1 = arrays[l - 3];
                                                }
                                                if(!tokenstats.contains("/")){
                                                    if(l - 5 > 0){
                                                        tokenstats = arrays[l - 5];
                                                        color1 = arrays[l - 4];
                                                    }
                                                }
                                                if(!tokenstats.contains("/")){
                                                    if(l - 6 > 0){
                                                        tokenstats = arrays[l - 6];
                                                        color1 = arrays[l - 5];
                                                        color2 = arrays[l - 3];
                                                    }
                                                }
                                                if(!tokenstats.contains("/")){
                                                    if(l - 7 > 0){
                                                        tokenstats = arrays[l - 7];
                                                        color1 = arrays[l - 6];
                                                        color2 = arrays[l - 4];
                                                    }
                                                }
                                                if(!tokenstats.contains("/")){
                                                    if(l - 8 > 0) {
                                                        tokenstats = arrays[l - 8];
                                                        color1 = arrays[l - 7];
                                                        color2 = arrays[l - 5];
                                                    }
                                                }    
                                            }
                                            if(!tokenstats.contains("/"))
                                                tokenstats = "";
                                            
                                            if(color1.toLowerCase().contains("white"))
                                                color1 = "W";
                                            else if(color1.toLowerCase().contains("blue"))
                                                color1 = "U";
                                            else if(color1.toLowerCase().contains("black"))
                                                color1 = "B";
                                            else if(color1.toLowerCase().contains("red"))
                                                color1 = "R";
                                            else if(color1.toLowerCase().contains("green"))
                                                color1 = "G";
                                            else if (color1.toLowerCase().contains("colorless"))
                                                color1 = "C";
                                            else 
                                                color1 = "";
                                            
                                            if(color2.toLowerCase().contains("white"))
                                                color2 = "W";
                                            else if(color1.toLowerCase().contains("blue"))
                                                color2 = "U";
                                            else if(color1.toLowerCase().contains("black"))
                                                color2 = "B";
                                            else if(color1.toLowerCase().contains("red"))
                                                color2 = "R";
                                            else if(color1.toLowerCase().contains("green"))
                                                color2 = "G";
                                            else 
                                                color2 = "";
                                            
                                            if(!color1.isEmpty()){
                                                color = "(" + color1 + color2 + ")";
                                            }
                                            break;
                                        } else if (arrays[l].equalsIgnoreCase("put") && arrays[l + 3].toLowerCase().contains("token")) {
                                            nametoken = arrays[l + 2];
                                            for (int j = 1; j < arrays.length - 1; j++) {
                                                if (arrays[j].contains("/")){
                                                    tokenstats = arrays[j];
                                                    color = arrays[j+1];
                                                }
                                            }
                                            if(color.toLowerCase().contains("white"))
                                                color = "(W)";
                                            else if(color.toLowerCase().contains("blue"))
                                                color = "(U)";
                                            else if(color.toLowerCase().contains("black"))
                                                color = "(B)";
                                            else if(color.toLowerCase().contains("red"))
                                                color = "(R)";
                                            else if(color.toLowerCase().contains("green"))
                                                color = "(G)";
                                            else if (color.toLowerCase().contains("colorless"))
                                                color = "(C)";
                                            else 
                                                color = "";
                                            break;
                                        }
                                    }
                                    String specialtokenurl = getSpecialTokenUrl(id + "t");
                                    Elements imgstoken;
                                    if(!specialtokenurl.isEmpty()) {
                                        try{
                                            doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                        } catch (Exception ex) {
                                            setTextArea("Error: Problem occurring while searching for token: " + nametoken + " (" + id + "t.jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                            break;
                                        }
                                        if(doc == null)
                                            break;
                                        imgstoken = doc.select("body img");
                                        if(imgstoken == null)
                                            break;
                                        tokenfound = true;
                                    } else {
                                        try{
                                            if (nametoken.isEmpty() || tokenstats.isEmpty()) {
                                                tokenfound = false;
                                                if(nametoken.isEmpty())
                                                    nametoken = "Unknown";
                                                nametocheck = mappa.get(id);
                                                doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                            } else {
                                                try {
                                                    tokenfound = true;
                                                    nametocheck = nametoken;
                                                    doc = findTokenPage(imageurl, nametoken, scryset, tokenstats, color);
                                                } catch(Exception e) {
                                                    tokenfound = false;
                                                    nametocheck = mappa.get(id);
                                                    doc = Jsoup.connect(imageurl + scryset.toLowerCase()).get();
                                                }
                                            }
                                        } catch(Exception e){
                                            setTextArea("Error: Problem occurring while searching for token: " + nametoken + " (" + id + "t.jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                            break;
                                        }
                                        if(doc == null)
                                            break;
                                        imgstoken = doc.select("body img");
                                        if(imgstoken == null)
                                            break;
                                    
                                    }
                                    for (int p = 0; p < imgstoken.size() && !interrupted; p++) {
                                        while(paused && !interrupted){
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {}
                                        }
                                        if(interrupted)
                                            break;
                                        String titletoken = imgstoken.get(p).attributes().get("alt");
                                        if(titletoken.isEmpty())
                                            titletoken = imgstoken.get(p).attributes().get("title");
                                        if (titletoken.toLowerCase().contains(nametocheck.toLowerCase())) {
                                            String CardImageToken = imgstoken.get(p).attributes().get("src");
                                            if (CardImageToken.isEmpty())
                                                CardImageToken = imgstoken.get(p).attributes().get("data-src");
                                            CardImageToken = CardImageToken.replace("/normal/", "/large/");
                                            URL urltoken = new URL(CardImageToken);
                                            if(!specialtokenurl.isEmpty())
                                                urltoken = new URL(specialtokenurl);
                                            
                                            if(cardlist != null){
                                                cardlist.append(currentSet + ";" + id + "t;" + urltoken.toString() + "\n");
                                                cardlist.flush();
                                            }
                                            
                                            HttpURLConnection httpcontoken = (HttpURLConnection) urltoken.openConnection();
                                            if(httpcontoken == null) {
                                                setTextArea("Error: Problem downloading token: " + nametoken + " (" + id + "t.jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                                break;
                                            }
                                            httpcontoken.addRequestProperty("User-Agent", "Mozilla/4.76");
                                            httpcontoken.setConnectTimeout(5000);
                                            httpcontoken.setReadTimeout(5000);
                                            httpcontoken.setAllowUserInteraction(false);
                                            httpcontoken.setDoInput(true);
                                            httpcontoken.setDoOutput(false);
                                            InputStream intoken = null;
                                            try{
                                                intoken = new BufferedInputStream(httpcontoken.getInputStream());
                                            }catch(Exception ex){
                                                setTextArea("Warning: Problem downloading token: " + nametoken + " (" + id + "t.jpg), i will retry 2 times more...", Color.blue, new Font("Arial", 1, 14));
                                                try {
                                                    intoken = new BufferedInputStream(httpcontoken.getInputStream());
                                                } catch (Exception ex2) {
                                                    setTextArea("Warning: Problem downloading token: " + nametoken + " (" + id + "t.jpg), i will retry 1 time more...", Color.blue, new Font("Arial", 1, 14));
                                                    try {
                                                        intoken = new BufferedInputStream(httpcontoken.getInputStream());
                                                    } catch (Exception ex3) {
                                                        setTextArea("Error: Problem downloading token: " + nametoken + " (" + id + "t.jpg), i will not retry anymore...", Color.red, new Font("Arial", 1, 14));
                                                        break;
                                                    }
                                                }
                                            }

                                            if(!tokenfound && !id.equals("464007")){
                                                setTextArea("Error: Problem downloading token: " + nametoken + " (" + id + "t.jpg) i will use the same image of its source card", Color.red, new Font("Arial", 1, 14));
                                            }
                                            
                                            String tokenimage = imgPath + File.separator + id + "t.jpg";
                                            String tokenthumbimage = thumbPath + File.separator + id + "t.jpg";
                                            
                                            try {
                                                ReadableByteChannel readableByteChannel2 = Channels.newChannel(httpcontoken.getInputStream());
                                                FileOutputStream fileOutputStream2 = new FileOutputStream(tokenimage);
                                                FileChannel fileChannel2 = fileOutputStream2.getChannel();
                                                fileOutputStream2.getChannel().transferFrom(readableByteChannel2, 0, Long.MAX_VALUE);
                                                fileChannel2.close();
                                                fileOutputStream2.close();

                                                fileOutputStream2 = new FileOutputStream(tokenthumbimage);
                                                fileChannel2 = fileOutputStream2.getChannel();
                                                fileOutputStream2.getChannel().transferFrom(readableByteChannel2, 0, Long.MAX_VALUE);
                                                fileChannel2.close();
                                                fileOutputStream2.close();
                                                readableByteChannel2.close();
                                           
                                                Toolkit toolkitToken = Toolkit.getDefaultToolkit();
                                                MediaTracker trackerToken = new MediaTracker(new Panel());
                                                Image imageToken = toolkitToken.getImage(tokenimage);
                                                trackerToken.addImage(imageToken, 0);
                                                try {
                                                    trackerToken.waitForAll();
                                                } catch (Exception e) { }
                                            
                                                BufferedImage resizedImgToken = new BufferedImage(ImgX, ImgY, BufferedImage.TYPE_INT_RGB);
                                                Graphics2D tGraphics2DReiszedToken = resizedImgToken.createGraphics(); //create a graphics object to paint to
                                                if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                                        currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                                        currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                                        currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                                    tGraphics2DReiszedToken.setBackground(Color.WHITE);
                                                    tGraphics2DReiszedToken.setPaint(Color.WHITE);
                                                }else {
                                                    tGraphics2DReiszedToken.setBackground(Color.BLACK);
                                                    tGraphics2DReiszedToken.setPaint(Color.BLACK);
                                                }
                                                tGraphics2DReiszedToken.fillRect(0, 0, ImgX, ImgY);
                                                tGraphics2DReiszedToken.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                                tGraphics2DReiszedToken.drawImage(imageToken, 0, 0, ImgX, ImgY, null); //draw the image scaled
                                                resizedImgToken = resizedImgToken.getSubimage(Border, Border, ImgX-2*Border, ImgY-2*Border);
                                                ImageIO.write(resizedImgToken, "JPG", new File(tokenimage)); //write the image to a file

                                                BufferedImage tThumbImageToken = new BufferedImage(ThumbX, ThumbY, BufferedImage.TYPE_INT_RGB);
                                                Graphics2D tGraphics2DToken = tThumbImageToken.createGraphics(); //create a graphics object to paint to
                                                if(currentSet.equals("2ED") || currentSet.equals("RV") || currentSet.equals("4ED") || currentSet.equals("5ED") || 
                                                        currentSet.equals("6ED") || currentSet.equals("7ED") || currentSet.equals("8ED") || currentSet.equals("9ED") || 
                                                        currentSet.equals("CHR") || currentSet.equals("DM") || currentSet.equals("S00") || currentSet.equals("S99") || 
                                                        currentSet.equals("PTK") || currentSet.equals("BTD") || currentSet.equals("ATH") || currentSet.equals("BRB")){
                                                    tGraphics2DToken.setBackground(Color.WHITE);
                                                    tGraphics2DToken.setPaint(Color.WHITE);
                                                }else {
                                                    tGraphics2DToken.setBackground(Color.BLACK);
                                                    tGraphics2DToken.setPaint(Color.BLACK);
                                                }
                                                tGraphics2DToken.fillRect(0, 0, ThumbX, ThumbY);
                                                tGraphics2DToken.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                                tGraphics2DToken.drawImage(imageToken, 0, 0, ThumbX, ThumbY, null); //draw the image scaled
                                                tThumbImageToken = tThumbImageToken.getSubimage(BorderThumb, BorderThumb, ThumbX-2*BorderThumb, ThumbY-2*BorderThumb);
                                                ImageIO.write(tThumbImageToken, "JPG", new File(tokenthumbimage)); //write the image to a file
                                            } catch (Exception e){
                                                setTextArea("Error: Problem saving token: " + nametoken + " (" + id + "t.jpg), i will not download it...", Color.red, new Font("Arial", 1, 14));
                                                break;
                                            }
                                            
                                            break;
                                        }
                                    }
                                }
                                break;
                            }    
                        }
                    }
                    if(!interrupted){
                        while(paused && !interrupted){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}
                        }
                        if(interrupted)
                            break;
                        try {
                            File oldzip = new File(destinationPath + File.separator + currentSet + File.separator + currentSet + ".zip");
                            oldzip.delete();
                        } catch (Exception e) {}
                        if(zipped){
                            try {
                                ZipParameters zipParameters = new ZipParameters();
                                zipParameters.setCompressionMethod(CompressionMethod.STORE);
                                File folder = new File(destinationPath + currentSet + File.separator);
                                File[] listOfFileZip = folder.listFiles();
                                net.lingala.zip4j.ZipFile zipped = new net.lingala.zip4j.ZipFile(destinationPath + File.separator + currentSet + File.separator + currentSet + ".zip");
                                for (int i = 0 ; i < listOfFileZip.length; i++){
                                    if(listOfFileZip[i].isDirectory()){
                                        zipped.addFolder(listOfFileZip[i],zipParameters);
                                    } else {
                                        zipped.addFile(listOfFileZip[i], zipParameters);
                                    }
                                }
                                File destFolder = new File(destinationPath + currentSet + File.separator);
                                File [] listOfFiles = destFolder.listFiles();
                                for(int u = 0; u < listOfFiles.length; u++){
                                    if (!listOfFiles[u].getName().contains(".zip")){
                                        if(listOfFiles[u].isDirectory()){
                                            File[] listOfSubFiles = listOfFiles[u].listFiles();
                                            for(int j = 0; j < listOfSubFiles.length; j++)
                                                listOfSubFiles[j].delete();
                                        }
                                        listOfFiles[u].delete();
                                    }
                                }
                                setTextArea("File " + destinationPath + currentSet + ".zip has been created", Color.black, new Font("Arial", 1, 14));
                            } catch (Exception e) {
                                setTextArea(e.getMessage(), Color.red, new Font("Arial", 1, 14));
                                setTextArea("Error creating file " + destinationPath + currentSet + ".zip", Color.red, new Font("Arial", 1, 14));
                                setTextArea("Warning: Images will not be compressed in a zip archive", Color.blue, new Font("Arial", 1, 14));
                            }
                        }
                        setTextArea("Download of " + currentSet + " completed", Color.green, new Font("Arial", 1, 14));
                    }
                }
                if(!interrupted){
                    setTextArea("All requested sets have been downloaded", Color.green, new Font("Arial", 1, 14));
                    getProgressBar().setStringPainted(true);
                    getProgressBar().setString("");
                    getProgressBar().setValue(0);
                    if(cardlist != null){
                        cardlist.flush();
                        cardlist.close();
                    }
                }
                if(logger != null) {
                    logger.flush();
                    logger.close();
                }
            } catch (Exception e) {
                setTextArea("Error: " + e.getCause().toString() + " - " + e.getMessage(), Color.red, new Font("Arial", 1, 14));
                if(logger != null){
                    logger.flush();
                    logger.close();
                }
            }
            paused = false;
            interrupted = false;
            jButton1.setText("Start Download");
        }
    };
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jComboBox1 = new CheckedComboBox();
        jLabel1 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton4 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jButton5 = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Wagic Card Image Downloader");
        setLocationByPlatform(true);
        setResizable(false);

        jTextField1.setEditable(false);
        jTextField1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        jComboBox1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Choose Set:");

        jComboBox2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "High", "Medium", "Low", "Tiny" }));

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Target Res:");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("Wagic Path:");

        jCheckBox1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Zip After Download");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Download Details:");

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton1.setText("Start Download");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton2.setText("Exit Downloader");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton3.setText("Clean Detalis");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("...");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jScrollPane2.setViewportView(jTextPane1);

        jButton5.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton5.setText("Stop Download");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jCheckBox2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jCheckBox2.setText("Borderless");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(377, 377, 377)
                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBox1))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 636, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jScrollPane2)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jCheckBox1)
                    .addComponent(jCheckBox2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addGap(7, 7, 7)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addGap(5, 5, 5)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton5))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if(downloader == null || jButton1.getText().equals("Start Download")){
            SelectedSets = new ArrayList<>();
            if(((CheckableItem)(jComboBox1).getItemAt(0)).selected) {
                for(int i = 1; i < jComboBox1.getItemCount(); i++)
                    SelectedSets.add(((CheckableItem)(jComboBox1).getItemAt(i)).toString().split(" - ")[0]);
            }
            else {
                for(int i = 0; i < jComboBox1.getItemCount(); i++){
                    if(((CheckableItem)(jComboBox1).getItemAt(i)).selected)
                        SelectedSets.add(((CheckableItem)(jComboBox1).getItemAt(i)).toString().split(" - ")[0]);
                }
            }
            targetres = jComboBox2.getSelectedItem().toString();
            WagicPath = jTextField1.getText();
            zipped = jCheckBox1.isSelected();
            borderless = jCheckBox2.isSelected();
            try {
                String path = new File(DownloaderGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                if(path.contains(".jar"))
                    path = path.substring(0,path.length() - 24);
                logger = new PrintWriter(path + File.separator + "DownloadCycle_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt");
            } catch (Exception ex) {
                logger = null;
            }
            downloader = new Thread(Downloader);
            downloader.start();
            paused = false;
            interrupted = false;
            jButton1.setText("Pause Download");
        } else if(jButton1.getText().equals("Pause Download")) {
            paused = true;
            interrupted = false;
            jButton1.setText("Resume Download");
            setTextArea("\nWarning: The download process has been paused from user...\n", Color.blue, new Font("Arial", 1, 14));
            
        } else if(jButton1.getText().equals("Resume Download")) {
            paused = false;
            interrupted = false;
            jButton1.setText("Pause Download");
            setTextArea("\nWarning: The download process has been resumed from user...\n", Color.blue, new Font("Arial", 1, 14));
            
        }
    }//GEN-LAST:event_jButton1ActionPerformed
    
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        getTextArea().setText("");
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        final JFileChooser f = new JFileChooser();
        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.setDialogTitle("Choose Wagic Installation Folder");
        f.setApproveButtonText("Confirm");
        f.showOpenDialog(null);
        File baseFolder = new File(f.getSelectedFile().getAbsolutePath() + File.separator + "Res" + File.separator);
        File [] listOfFiles = baseFolder.listFiles();
        final ArrayList<String> sets = new ArrayList<>();
        boolean isZipped = false;
        getProgressBar().setValue(0);
        getProgressBar().setStringPainted(true);
        if (listOfFiles != null && listOfFiles.length == 1){
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(baseFolder + File.separator + listOfFiles[0].getName());
                Enumeration<? extends ZipEntry> e = zipFile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String entryName = entry.getName();
                    if(entryName != null && entryName.contains("sets/")){
                        if(!entryName.equalsIgnoreCase("sets/") && !entryName.contains("primitives") && !entryName.contains(".")){
                            String[] names = entryName.split("/");
                            sets.add(names[1]);
                        }
                    }
                }
                isZipped = true;
            }
            catch (Exception ioe) {
               setTextArea("Error opening Res zip file: " + ioe, Color.red, new Font("Arial", 1, 14));
            }
            finally {
                try {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                }
                catch (Exception ioe) {
                    setTextArea("Error while closing Res zip file: " + ioe, Color.red, new Font("Arial", 1, 14));
                }
            }
        } else if(listOfFiles != null && listOfFiles.length > 1) {
            File setsFolder = new File(f.getSelectedFile().getAbsolutePath() + File.separator + "Res" + File.separator + "sets" + File.separator);
            File [] listOfSet = setsFolder.listFiles();
            if(listOfSet != null){
                getProgressBar().setMaximum(listOfSet.length);
                for (int t = 0; t < listOfSet.length; t++) {
                    if(listOfSet[t].isDirectory() && !listOfSet[t].getName().equalsIgnoreCase("primitives"))
                        sets.add(listOfSet[t].getName());
                }
            }
        }
        if(sets.size() > 0) {
            getProgressBar().setMaximum(sets.size());
            jTextField1.setText(f.getSelectedFile().getAbsolutePath());
            jComboBox1.removeAllItems();
            final boolean toZip = isZipped;
            jComboBox1.addItem(new CheckableItem("*.* - All Wagic sets (thousands of cards)", true));
            new Thread(new Runnable(){
                @Override
                public void run(){
                    for(int i = 0; i < sets.size(); i++){
                        getProgressBar().setString("Loading set: " + sets.get(i));
                        getProgressBar().setValue(i+1);
                        getSetCombo().addItem(new CheckableItem(sets.get(i) + " - " +  getSetInfo(sets.get(i), toZip, f.getSelectedFile().getAbsolutePath()), false));
                    }
                    getProgressBar().setValue(0);
                    getProgressBar().setString("");
                }
            }).start();
        } else {
            jTextField1.setText("Wrong installation path, sets folder not found...");
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        if(downloader != null && !jButton1.getText().equals("Start Download")){
            paused = false;
            interrupted = true;
            setTextArea("\nWarning: The download process has been interruped from user...\n", Color.blue, new Font("Arial", 1, 14));
            jButton1.setText("Start Download");
            if(logger != null) {
                logger.flush();
                logger.close();
            }
            if(cardlist != null) {
                cardlist.flush();
                cardlist.close();
            }
            getProgressBar().setValue(0);
            getProgressBar().setString("");
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private CheckedComboBox jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
