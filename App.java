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
        "今天的科技新闻真有意思，{keyword} 又有新进展！",
        "这条关于 {keyword} 的新闻，感觉未来已来。",
        "{keyword} 这个话题最近很火，值得关注。"
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
                html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
                html.append("<meta name='referrer' content='no-referrer'>");
                html.append("<title>TechNews 即时新闻</title>");
                html.append("<style>");
                html.append("*{margin:0;padding:0;box-sizing:border-box;}");
                html.append("body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f5;max-width:900px;margin:0 auto;padding:20px;}");
                html.append(".header{text-align:center;padding:30px 0;border-bottom:3px solid #2196F3;margin-bottom:20px;}");
                html.append(".header h1{font-size:2em;color:#1565C0;}");
                html.append(".header p{color:#666;margin-top:5px;}");
                html.append(".comment-box{background:#E3F2FD;padding:15px;border-radius:8px;margin-bottom:20px;font-size:1.05em;}");
                html.append(".news-card{background:#fff;border-radius:10px;display:flex;margin-bottom:20px;");
                html.append("box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;transition:transform 0.2s;}");
                html.append(".news-card:hover{transform:translateY(-2px);box-shadow:0 4px 12px rgba(0,0,0,0.15);}");
                html.append(".img-container{width:200px;min-width:200px;overflow:hidden;}");
                html.append(".news-img{width:100%;height:150px;object-fit:cover;transition:transform 0.3s;cursor:pointer;}");
                html.append(".news-img:hover{transform:scale(1.2);}");
                html.append(".news-img.zoomed{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%) scale(2.5);z-index:999;border-radius:10px;}");
                html.append(".news-content{padding:15px;flex:1;}");
                html.append(".news-content h3{margin-bottom:8px;}");
                html.append(".news-content h3 a{color:#1565C0;text-decoration:none;}");
                html.append(".news-content h3 a:hover{text-decoration:underline;}");
                html.append(".summary{color:#555;margin:8px 0;line-height:1.5;}");
                html.append(".source{display:inline-block;background:#e3f2fd;color:#1565C0;padding:3px 10px;border-radius:12px;font-size:0.85em;margin-top:5px;}");
                html.append(".spell-box{margin-top:40px;padding:25px;background:#fff;border-radius:10px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.1);}");
                html.append(".spell-box h3{margin-bottom:15px;color:#333;}");
                html.append(".spell-box input{padding:10px 15px;width:250px;font-size:1em;border:2px solid #ddd;border-radius:5px;outline:none;}");
                html.append(".spell-box input:focus{border-color:#2196F3;}");
                html.append(".spell-box button{padding:10px 20px;background:#2196F3;color:#fff;border:none;border-radius:5px;cursor:pointer;margin-left:10px;font-size:1em;}");
                html.append(".spell-box button:hover{background:#1976D2;}");
                html.append("#result{margin-top:15px;font-size:1.1em;}");
                html.append(".footer{text-align:center;margin-top:40px;padding:20px;color:#999;font-size:0.9em;border-top:1px solid #ddd;}");
                html.append("@media(max-width:600px){.news-card{flex-direction:column;}.img-container{width:100%;height:200px;}}");
                html.append("</style></head><body>");
                
                html.append("<div class='header'><h1>📰 TechNews 即时新闻</h1>");
                html.append("<p>Guardian · CNN · BBC 实时爬取</p></div>");
                // 音乐播放按钮
html.append("<div style='text-align:center;margin-bottom:15px;'>");
html.append("<button id='musicBtn' onclick='toggleMusic()' style='background:#4CAF50;color:white;border:none;padding:8px 20px;border-radius:20px;cursor:pointer;font-size:14px;'>🎵 播放音乐</button>");
html.append("</div>");
                
                String comment = generateComment(news);
                html.append("<div class='comment-box'><p>💬 ").append(comment).append("</p></div>");
                
                for (NewsItem item : news) {
                    html.append("<div class='news-card'>");
                    if(item.imageUrl!=null && !item.imageUrl.isEmpty()) {
                        html.append("<div class='img-container'><img src='").append(item.imageUrl)
                            .append("' class='news-img' onerror=\"this.onerror=null;this.src='https://placehold.co/400x300/e0e0e0/999?text=No+Image';\"")
                            .append(" onclick='this.classList.toggle(\"zoomed\")'></div>");
                    } else {
                        html.append("<div class='img-container' style='background:#e0e0e0;display:flex;")
                            .append("align-items:center;justify-content:center;height:150px;'>")
                            .append("<span style='color:#999;'>暂无图片</span></div>");
                    }
                    html.append("<div class='news-content'>");
                    html.append("<h3><a href='").append(item.link).append("' target='_blank'>")
                        .append(item.title).append("</a></h3>");
                    html.append("<p class='summary'>").append(item.getShortSummary()).append("</p>");
                    html.append("<span class='source'>📍 ").append(item.source).append("</span>");
                    html.append("</div></div>");
                }
                
                html.append("<div class='spell-box'>");
                html.append("<h3>📝 线上拼字检查</h3>");
                html.append("<input id='word' placeholder='输入英文单字' autofocus>");
                html.append("<button onclick=\"checkSpelling()\">检查拼字</button>");
                html.append("<p id='result'></p>");
                html.append("</div>");
                
                html.append("<div class='footer'><p>🔄 数据即时从 Guardian API · CNN RSS · BBC RSS 爬取</p></div>");
                
                html.append("<script>");
                // 音乐播放功能
html.append("var audio = null;");
html.append("function toggleMusic(){");
html.append("if(!audio){");
html.append("audio = new Audio('https://www.qtings.com/uploads/tracks/2124019576_786842529_1597766027.mp3');");  // 替换成你的音乐链接
html.append("audio.loop = true;");
html.append("audio.play();");
html.append("document.getElementById('musicBtn').innerHTML = '⏸️ 暂停音乐';");
html.append("} else if(audio.paused){");
html.append("audio.play();");
html.append("document.getElementById('musicBtn').innerHTML = '⏸️ 暂停音乐';");
html.append("} else {");
html.append("audio.pause();");
html.append("document.getElementById('musicBtn').innerHTML = '🎵 播放音乐';");
html.append("}");
html.append("}");
                html.append("function checkSpelling(){");
                html.append("var word=document.getElementById('word').value.trim();");
                html.append("if(!word){document.getElementById('result').innerHTML='请输入单词';return;}");
                html.append("fetch('https://api.datamuse.com/words?sp='+word+'&max=1')");
                html.append(".then(r=>r.json())");
                html.append(".then(d=>{");
                html.append("var r=document.getElementById('result');");
                html.append("if(d.length&&d[0].word==word.toLowerCase())r.innerHTML='✅ <b>'+word+'</b> 拼写正确！';");
                html.append("else if(d.length)r.innerHTML='❌ 找不到 <b>'+word+'</b>，您是否要找：<b>'+d[0].word+'</b>？';");
                html.append("else r.innerHTML='❌ 找不到 <b>'+word+'</b>';");
                html.append("}).catch(e=>{document.getElementById('result').innerHTML='检查出错，请稍后再试';});");
                html.append("}");
                html.append("document.getElementById('word').addEventListener('keypress',function(e){if(e.key==='Enter')checkSpelling();});");
                html.append("</script>");
                
                html.append("</body></html>");
                
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
        System.out.println("TechNews 服务器已启动，端口: " + port);
    }
    
    static String generateComment(List<NewsItem> news) {
        if(news.isEmpty()) return "暂无新闻数据。";
        NewsItem item = news.get(new Random().nextInt(news.size()));
        String[] words = item.title.split(" ");
        String kw = words.length>2 ? words[words.length/2] : "科技";
        return comments[new Random().nextInt(comments.length)].replace("{keyword}", kw);
    }
    
    // ==================== Guardian API ====================
    static List<NewsItem> fetchGuardian() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        String apiKey = "5bc21cfa-ec62-47ba-a575-6124bb4b5a81";
        String url = "https://content.guardianapis.com/search?api-key="+apiKey+
                     "&section=technology&show-fields=thumbnail,trailText&page-size=10";
        Thread.sleep(2000);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "StudentProject/1.0");
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder(); String line;
        while((line=r.readLine())!=null) json.append(line); r.close();
        
        String[] parts = json.toString().split("\"webTitle\":\"");
        for(int i=1;i<parts.length;i++) {
            String title = parts[i].split("\"")[0].replace("\\\"", "\"");
            String link=""; int u=parts[i].indexOf("\"webUrl\":\"");
            if(u>=0) link=parts[i].substring(u+10).split("\"")[0];
            
            String img = "";
            int im = parts[i].indexOf("\"thumbnail\":\"");
            if(im >= 0) img = parts[i].substring(im+13).split("\"")[0];
            
            String summary=""; int t=parts[i].indexOf("\"trailText\":\"");
            if(t>=0) summary=parts[i].substring(t+13).split("\"")[0].replace("\\\"", "\"");
            
            list.add(new NewsItem(title,summary,link,img,"The Guardian"));
        }
        return list;
    }
    
    // ==================== CNN RSS ====================
    static List<NewsItem> fetchCNN() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        Thread.sleep(3000);
        Document doc = Jsoup.connect("http://rss.cnn.com/rss/edition_technology.rss")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .timeout(10000).ignoreContentType(true).get();
        
        for(Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary=""; 
            Element desc=item.select("description").first();
            if(desc!=null) { 
                summary=Jsoup.parse(desc.text()).text(); 
                if(summary.length()>100) summary=summary.substring(0,97)+"..."; 
            }
            
            String img = "";
            Element mediaContent = item.select("media\\:content").first();
            if(mediaContent != null) img = mediaContent.attr("url");
            if(img.isEmpty()) {
                Element enclosure = item.select("enclosure").first();
                if(enclosure != null) img = enclosure.attr("url");
            }
            if(img.isEmpty() && desc != null) {
                Element ie = Jsoup.parse(desc.text()).select("img").first();
                if(ie != null) img = ie.absUrl("src");
            }
            
            list.add(new NewsItem(title,summary,link,img,"CNN"));
        }
        return list;
    }
    
    // ==================== BBC RSS ====================
    static List<NewsItem> fetchBBC() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        Thread.sleep(3000);
        Document doc = Jsoup.connect("https://feeds.bbci.co.uk/news/technology/rss.xml")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .timeout(10000).ignoreContentType(true).get();
        
        for(Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary=""; 
            Element desc=item.select("description").first();
            if(desc!=null) { 
                summary=Jsoup.parse(desc.text()).text(); 
                if(summary.length()>100) summary=summary.substring(0,97)+"..."; 
            }
            
            String img = "";
            Element thumbnail = item.select("media\\:thumbnail").first();
            if(thumbnail != null) img = thumbnail.attr("url");
            if(img.isEmpty() && desc != null) {
                Element ie = Jsoup.parse(desc.text()).select("img").first();
                if(ie != null) img = ie.absUrl("src");
            }
            
            list.add(new NewsItem(title,summary,link,img,"BBC News"));
        }
        return list;
    }
}
