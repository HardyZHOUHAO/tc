import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
        "Today's tech news is interesting, {keyword} has new progress!",
        "This news about {keyword} feels like the future is here.",
        "{keyword} is a hot topic lately, worth paying attention to."
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
                html.append("<title>TechNews</title>");
                html.append("<style>");
                html.append("*{margin:0;padding:0;box-sizing:border-box;}");
                html.append("body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f5;max-width:1000px;margin:0 auto;padding:20px;}");
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
                html.append(".dictionary-box{margin-top:40px;padding:25px;background:#fff;border-radius:10px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.1);}");
                html.append(".dictionary-box h3{margin-bottom:15px;color:#333;}");
                html.append(".dictionary-box input{padding:10px 15px;width:280px;font-size:1em;border:2px solid #ddd;border-radius:5px;outline:none;}");
                html.append(".dictionary-box input:focus{border-color:#2196F3;}");
                html.append(".dictionary-box button{padding:10px 20px;background:#2196F3;color:#fff;border:none;border-radius:5px;cursor:pointer;margin-left:10px;font-size:1em;}");
                html.append(".dictionary-box button:hover{background:#1976D2;}");
                html.append("#result{margin-top:20px;font-size:1em;line-height:1.6;text-align:left;max-width:500px;margin-left:auto;margin-right:auto;}");
                html.append(".result-card{background:#f8f9fa;border-radius:10px;padding:15px;margin-top:10px;}");
                html.append(".part-of-speech{display:inline-block;background:#4CAF50;color:white;padding:3px 10px;border-radius:15px;font-size:0.85em;margin:5px 5px 0 0;}");
                html.append(".definition{color:#555;margin-bottom:8px;padding-left:10px;border-left:3px solid #2196F3;}");
                html.append(".example{color:#888;font-style:italic;font-size:0.9em;margin-top:5px;padding-left:10px;}");
                html.append(".footer{text-align:center;margin-top:40px;padding:20px;color:#999;font-size:0.9em;border-top:1px solid #ddd;}");
                html.append(".loading{color:#666;text-align:center;padding:20px;}");
                html.append("@media(max-width:600px){.news-card{flex-direction:column;}.img-container{width:100%;height:200px;}}");
                html.append("</style></head><body>");
                
                html.append("<div class='header'><h1>📰 TechNews</h1>");
                html.append("<p>Guardian · CNN · BBC Real-time News</p></div>");
                
                html.append("<div style='text-align:center;margin-bottom:15px;'>");
                html.append("<button id='musicBtn' onclick='toggleMusic()' style='background:#4CAF50;color:white;border:none;padding:8px 20px;border-radius:20px;cursor:pointer;font-size:14px;'>🎵 Play Music</button>");
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
                            .append("<span style='color:#999;'>No Image</span></div>");
                    }
                    html.append("<div class='news-content'>");
                    html.append("<h3><a href='").append(item.link).append("' target='_blank'>")
                        .append(item.title).append("</a></h3>");
                    html.append("<p class='summary'>").append(item.getShortSummary()).append("</p>");
                    html.append("<span class='source'>📍 ").append(item.source).append("</span>");
                    html.append("</div></div>");
                }
                
                html.append("<div class='dictionary-box'>");
                html.append("<h3>📝 Online Dictionary</h3>");
                html.append("<p style='color:#666;margin-bottom:15px;font-size:0.9em;'>Enter an English word to check part of speech and definition</p>");
                html.append("<input id='word' placeholder='Enter a word, e.g. algorithm' autofocus>");
                html.append("<button onclick='checkDictionary()'>Search</button>");
                html.append("<div id='result'></div>");
                html.append("</div>");
                
                html.append("<div class='footer'><p>🔄 Data from Guardian API · CNN RSS · BBC RSS</p></div>");
                
                html.append("<script>");
                html.append("var audio = null;");
                html.append("function toggleMusic(){");
                html.append("if(!audio){");
                html.append("audio = new Audio('https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3');");
                html.append("audio.loop = true;");
                html.append("audio.play();");
                html.append("document.getElementById('musicBtn').innerHTML = '⏸️ Pause Music';");
                html.append("} else if(audio.paused){");
                html.append("audio.play();");
                html.append("document.getElementById('musicBtn').innerHTML = '⏸️ Pause Music';");
                html.append("} else {");
                html.append("audio.pause();");
                html.append("document.getElementById('musicBtn').innerHTML = '🎵 Play Music';");
                html.append("}");
                html.append("}");
                html.append("async function checkDictionary(){");
                html.append("var word=document.getElementById('word').value.trim().toLowerCase();");
                html.append("var resultDiv=document.getElementById('result');");
                html.append("if(!word){resultDiv.innerHTML='<div class=\"result-card\">⚠️ Please enter a word</div>';return;}");
                html.append("resultDiv.innerHTML='<div class=\"loading\">📖 Searching...</div>';");
                html.append("try{");
                html.append("var response=await fetch('/dict?word='+encodeURIComponent(word));");
                html.append("var data=await response.json();");
                html.append("if(data.error){resultDiv.innerHTML='<div class=\"result-card\">❌ '+data.error+'</div>';return;}");
                html.append("var html='<div class=\"result-card\"><strong>📖 '+data.word+'</strong>';");
                html.append("if(data.phonetic)html+='<div>🔊 /'+data.phonetic+'/</div>';");
                html.append("if(data.meanings && data.meanings.length>0){");
                html.append("data.meanings.forEach(function(m){");
                html.append("html+='<div><span class=\"part-of-speech\">'+m.partOfSpeech+'</span></div>';");
                html.append("m.definitions.forEach(function(def,idx){");
                html.append("html+='<div class=\"definition\">'+(idx+1)+'. '+def.definition+'</div>';");
                html.append("if(def.example)html+='<div class=\"example\">📌 \"'+def.example+'\"</div>';");
                html.append("});");
                html.append("});");
                html.append("}");
                html.append("resultDiv.innerHTML=html+'</div>';");
                html.append("}catch(e){resultDiv.innerHTML='<div class=\"result-card\">❌ Search failed</div>';}");
                html.append("}");
                html.append("document.getElementById('word').addEventListener('keypress',function(e){if(e.key==='Enter')checkDictionary();});");
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
        
        // Dictionary API endpoint
        server.createContext("/dict", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                String word = "";
                if (query != null && query.startsWith("word=")) {
                    word = URLDecoder.decode(query.substring(5), "UTF-8");
                }
                
                if (word.isEmpty()) {
                    String response = "{\"error\":\"Please enter a word\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                    return;
                }
                
                String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TechNews/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 404) {
                    String response = "{\"error\":\"Word not found, please check spelling\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(404, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                    return;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();
                
                String formattedData = parseDictionaryJson(json.toString(), word);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, formattedData.getBytes("UTF-8").length);
                exchange.getResponseBody().write(formattedData.getBytes("UTF-8"));
                exchange.close();
            } catch (Exception e) {
                String response = "{\"error\":\"Server error\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            }
        });
        
        server.start();
        System.out.println("TechNews server started on port: " + port);
    }
    
    static String generateComment(List<NewsItem> news) {
        if(news.isEmpty()) return "No news today.";
        NewsItem item = news.get(new Random().nextInt(news.size()));
        String[] words = item.title.split(" ");
        String kw = words.length>2 ? words[words.length/2] : "technology";
        return comments[new Random().nextInt(comments.length)].replace("{keyword}", kw);
    }
    
    static String parseDictionaryJson(String jsonStr, String word) {
        try {
            JsonArray arr = JsonParser.parseString(jsonStr).getAsJsonArray();
            if (arr.size() == 0) return "{\"error\":\"No definition found\"}";
            
            JsonObject firstEntry = arr.get(0).getAsJsonObject();
            StringBuilder result = new StringBuilder();
            result.append("{\"word\":\"").append(word).append("\",");
            
            if (firstEntry.has("phonetic") && !firstEntry.get("phonetic").isJsonNull()) {
                result.append("\"phonetic\":\"").append(escapeJson(firstEntry.get("phonetic").getAsString())).append("\",");
            } else {
                result.append("\"phonetic\":\"\",");
            }
            
            result.append("\"meanings\":[");
            JsonArray meanings = firstEntry.getAsJsonArray("meanings");
            for (int i = 0; i < meanings.size(); i++) {
                JsonObject meaning = meanings.get(i).getAsJsonObject();
                String partOfSpeech = meaning.get("partOfSpeech").getAsString();
                result.append("{\"partOfSpeech\":\"").append(partOfSpeech).append("\",");
                result.append("\"definitions\":[");
                
                JsonArray definitions = meaning.getAsJsonArray("definitions");
                for (int j = 0; j < Math.min(definitions.size(), 3); j++) {
                    JsonObject def = definitions.get(j).getAsJsonObject();
                    String definition = def.get("definition").getAsString();
                    result.append("{\"definition\":\"").append(escapeJson(definition)).append("\"");
                    if (def.has("example") && !def.get("example").isJsonNull()) {
                        result.append(",\"example\":\"").append(escapeJson(def.get("example").getAsString())).append("\"");
                    }
                    result.append("}");
                    if (j < Math.min(definitions.size(), 3) - 1) result.append(",");
                }
                result.append("]}");
                if (i < meanings.size() - 1) result.append(",");
            }
            result.append("]}");
            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"Parse failed\"}";
        }
    }
    
    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
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
            .userAgent("Mozilla/5.0")
            .timeout(10000).ignoreContentType(true).get();
        
        for (Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary = "";
            Element desc = item.select("description").first();
            if (desc != null) {
                summary = Jsoup.parse(desc.text()).text();
                if (summary.length() > 100) summary = summary.substring(0, 97) + "...";
            }
            
            String img = "";
            try {
                for (Element el : item.getAllElements()) {
                    if ("media:content".equals(el.tagName())) {
                        img = el.attr("url");
                        break;
                    }
                }
                if (img.isEmpty()) {
                    Element enclosure = item.select("enclosure").first();
                    if (enclosure != null) img = enclosure.attr("url");
                }
                if (img.isEmpty() && desc != null) {
                    Element ie = Jsoup.parse(desc.text()).select("img").first();
                    if (ie != null) img = ie.absUrl("src");
                }
            } catch (Exception e) {}
            
            list.add(new NewsItem(title, summary, link, img, "CNN"));
        }
        return list;
    }
    
    // ==================== BBC RSS ====================
    static List<NewsItem> fetchBBC() throws Exception {
        List<NewsItem> list = new ArrayList<>();
        Thread.sleep(3000);
        Document doc = Jsoup.connect("https://feeds.bbci.co.uk/news/technology/rss.xml")
            .userAgent("Mozilla/5.0")
            .timeout(10000).ignoreContentType(true).get();
        
        for (Element item : doc.select("item")) {
            String title = item.select("title").first().text();
            String link = item.select("link").first().text();
            String summary = "";
            Element desc = item.select("description").first();
            if (desc != null) {
                summary = Jsoup.parse(desc.text()).text();
                if (summary.length() > 100) summary = summary.substring(0, 97) + "...";
            }
            
            String img = "";
            try {
                for (Element el : item.getAllElements()) {
                    if ("media:thumbnail".equals(el.tagName())) {
                        img = el.attr("url");
                        break;
                    }
                }
                if (img.isEmpty() && desc != null) {
                    Element ie = Jsoup.parse(desc.text()).select("img").first();
                    if (ie != null) img = ie.absUrl("src");
                }
            } catch (Exception e) {}
            
            list.add(new NewsItem(title, summary, link, img, "BBC News"));
        }
        return list;
    }
}
