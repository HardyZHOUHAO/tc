import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {
    
    static class NewsItem {
        String title, summary, link, imageUrl, source;
        NewsItem(String t, String s, String l, String i, String src) {
            title=t; summary=s; link=l; imageUrl=i; source=src;
        }
        String getShortSummary() {
            if(summary==null||summary.isEmpty()) return "";
            return summary.length()>100 ? summary.substring(0,97)+"..." : summary;
        }
    }
    
    static String[] comments = {
        "Tech news is interesting today, {keyword} has new progress!",
        "This news about {keyword} makes me feel the future is here.",
        "{keyword} is a hot topic recently."
    };
    
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        
        server.createContext("/", exchange -> {
            try {
                List<NewsItem> news = new ArrayList<>();
                news.addAll(fetchGuardian());
                news.addAll(fetchCNN());
                news.addAll(fetchBBC());
                
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='referrer' content='no-referrer'><title>TechNews</title>");
                html.append("<style>");
                html.append("*{margin:0;padding:0;box-sizing:border-box;}");
                html.append("body{font-family:Arial;background:#f5f5f5;max-width:900px;margin:0 auto;padding:20px;}");
                html.append(".header{text-align:center;padding:30px 0;border-bottom:3px solid #2196F3;margin-bottom:20px;}");
                html.append(".header h1{font-size:2em;color:#1565C0;}");
                html.append(".comment-box{background:#E3F2FD;padding:15px;border-radius:8px;margin-bottom:20px;}");
                html.append(".sound-link{margin-bottom:20px;}");
                html.append(".news-card{background:#fff;border-radius:10px;display:flex;margin-bottom:20px;box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;}");
                html.append(".img-container{width:200px;min-width:200px;}");
                html.append(".news-img{width:100%;height:150px;object-fit:cover;transition:transform 0.3s;cursor:pointer;}");
                html.append(".news-img:hover{transform:scale(1.2);}");
                html.append(".news-img.zoomed{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%) scale(2.5);z-index:999;}");
                html.append(".news-content{padding:15px;}");
                html.append(".news-content h3 a{color:#1565C0;text-decoration:none;}");
                html.append(".summary{color:#666;margin:8px 0;}");
                html.append(".source{color:#999;font-size:0.85em;}");
                html.append(".spell-box{margin-top:40px;padding:20px;background:#fff;border-radius:10px;text-align:center;}");
                html.append(".spell-box input{padding:10px;width:250px;font-size:1em;}");
                html.append(".spell-box button{padding:10px 20px;background:#2196F3;color:#fff;border:none;border-radius:5px;cursor:pointer;margin-left:10px;}");
                html.append("@media(max-width:600px){.news-card{flex-direction:column;}.img-container{width:100%;}}");
                html.append("</style></head><body>");
                html.append("<div class='header'><h1>TechNews</h1><p>Guardian · CNN · BBC</p></div>");
                html.append("<div class='comment-box'><p>").append(generateComment(news)).append("</p></div>");
                html.append("<div class='sound-link'><a href='#' onclick=\"new Audio('https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3').play();return false;\">Sound</a></div>");
                
                for (NewsItem item : news) {
                    html.append("<div class='news-card'>");
                    if(item.imageUrl!=null && !item.imageUrl.isEmpty()) {
                        html.append("<div class='img-container'><img src='").append(item.imageUrl).append("' class='news-img' onerror=\"this.onerror=null;this.src='https://placehold.co/400x300/e0e0e0/999?text=No+Image';\" onclick='this.classList.toggle(\"zoomed\")'></div>");
                    } else {
                        html.append("<div class='img-container' style='background:#e0e0e0;display:flex;align-items:center;justify-content:center;height:150px;'><span style='color:#999;'>No Image</span></div>");
                    }
                    html.append("<div class='news-content'><h3><a href='").append(item.link).append("' target='_blank'>").append(item.title).append("</a></h3>");
                    html.append("<p class='summary'>").append(item.getShortSummary()).append("</p>");
                    html.append("<span class='source'>").append(item.source).append("</span></div></div>");
                }
                
                html.append("<div class='spell-box'><h3>Spell Check</h3>");
                html.append("<input id='word' placeholder='Enter word'>");
                html.append("<button onclick=\"fetch('https://api.datamuse.com/words?sp='+document.getElementById('word').value+'&max=1').then(r=>r.json()).then(d=>{var r=document.getElementById('result');if(d.length&&d[0].word==document.getElementById('word').value.toLowerCase())r.innerHTML='Correct';else if(d.length)r.innerHTML='Suggestion: '+d[0].word;else r.innerHTML='Not found';})\">Check</button>");
                html.append("<p id='result'></p></div></body></html>");
                
                byte[] bytes = html.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            } catch (Exception e) {
                String err = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, err.length());
                exchange.getResponseBody().write(err.getBytes());
                exchange.close();
            }
        });
        
        server.start();
        System.out.println("Running on port " + port);
    }
    
    static String generateComment(List<NewsItem> news) {
        if(news.isEmpty()) return "No news today.";
        NewsItem item = news.get(new Random().nextInt(news.size()));
        String[] words = item.title.split(" ");
        String kw = words.length>2 ? words[words.length/2] : "tech";
        return comments[new Random().nextInt(comments.length)].replace("{keyword}", kw);
    }
    
    static List<NewsItem> fetchGuardian() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        String apiKey = "5bc21cfa-ec62-47ba-a575-6124bb4b5a81";
        String url = "https://content.guardianapis.com/search?api-key="+apiKey+"&section=technology&show-fields=thumbnail,trailText&page-size=10";
        Thread.sleep(2000);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "StudentProject/1.0");
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder(); String line;
        while((line=r.readLine())!=null) json.append(line); r.close();
        String[] parts = json.toString().split("\"webTitle\":\"");
        for(int i=1;i<parts.length;i++) {
            String title = parts[i].split("\"")[0];
            String link=""; int u=parts[i].indexOf("\"webUrl\":\"");
            if(u>=0) link=parts[i].substring(u+10).split("\"")[0];
            String img = "";
            int im = parts[i].indexOf("\"thumbnail\":\"");
            if(im >= 0) {
                String thumb = parts[i].substring(im+14).split("\"")[0];
                if(thumb != null && !thumb.isEmpty()) img = thumb;
            }
            if(img.isEmpty() && !link.isEmpty()) {
                try {
                    Thread.sleep(1500);
                    Document article = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .timeout(8000).get();
                    Element ogImg = article.select("meta[property='og:image']").first();
                    if(ogImg != null) img = ogImg.attr("content");
                } catch (Exception e) { }
            }
            String summary=""; int t=parts[i].indexOf("\"trailText\":\"");
            if(t>=0) summary=parts[i].substring(t+13).split("\"")[0];
            list.add(new NewsItem(title,summary,link,img,"The Guardian"));
        }
        return list;
    }
    
    static List<NewsItem> fetchCNN() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        Thread.sleep(3000);
        Document doc = Jsoup.connect("http://rss.cnn.com/rss/edition_technology.rss")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(10000).ignoreContentType(true).get();
        for(Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary=""; Element desc=item.select("description").first();
            if(desc!=null) { summary=Jsoup.parse(desc.text()).text(); if(summary.length()>100) summary=summary.substring(0,97)+"..."; }
            String img=""; if(desc!=null) { Element ie=Jsoup.parse(desc.text()).select("img").first(); if(ie!=null) img=ie.absUrl("src"); }
            list.add(new NewsItem(title,summary,link,img,"CNN"));
        }
        return list;
    }
    
    static List<NewsItem> fetchBBC() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        Thread.sleep(3000);
        Document doc = Jsoup.connect("https://feeds.bbci.co.uk/news/technology/rss.xml")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(10000).ignoreContentType(true).get();
        for(Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary=""; Element desc=item.select("description").first();
            if(desc!=null) { summary=Jsoup.parse(desc.text()).text(); if(summary.length()>100) summary=summary.substring(0,97)+"..."; }
            String img=""; if(desc!=null) { Element ie=Jsoup.parse(desc.text()).select("img").first(); if(ie!=null) img=ie.absUrl("src"); }
            list.add(new NewsItem(title,summary,link,img,"BBC News"));
        }
        return list;
    }
}
