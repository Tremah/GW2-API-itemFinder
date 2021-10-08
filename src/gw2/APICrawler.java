package gw2;
import java.io.*;
import java.util.HashMap;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.*;


public class APICrawler {
    //API
    private final static String API_ENTRY = "https://api.guildwars2.com/v2/items";
    private final static String API_ITEM_SUFFIX = "?ids=";

    //application config
    private static HashMap<String, String> configList = new HashMap<String, String>();
    private final static String itemDbPath = "db\\db.csv";
    private final static String configFilePath = "config\\config.txt";
    private final static int SLEEP_AMOUNT = 500;
    private final static int MAX_ID_COUNT = 200;


    private static int numItems = 0;

    public static boolean initApp() throws IOException {
        System.out.println("Initializing application...");
        BufferedReader bfr = readFromFile(configFilePath);
        StringBuffer sbfr = new StringBuffer();
        String line = "";
        while((line = bfr.readLine()) != null){
            //exclude empty lines and comments (lines starting with a "#"
            if(line.length() > 0 && !(line.substring(0, 1).equals("#"))){
                //prepare line string
                line = line.trim();
                line = line.replace("\n", "");
                //find position of = to separate key-value pair
                int posSeparator = line.indexOf("=");
                if(posSeparator == -1){
                    //no "=" found
                    System.out.println("not able to parse line: " + line);
                }
                else{
                    //process line and insert into list of config values
                    String key = line.substring(0, posSeparator);
                    String value = line.substring(posSeparator + 1);
                    configList.put(key, value);
                    System.out.println("read key value pair: " + key + " = " + value);
                }
            }
        }

        return false;
    }

    public static BufferedReader getApiResponse(String url) throws IOException {
        //return Buffered Reader to read from API
        URL itemUrl = new URL(url);
        HttpURLConnection itemConn = (HttpURLConnection) itemUrl.openConnection();
        itemConn.setRequestMethod("GET");
        itemConn.setRequestProperty("Content-Type", "application/json");
        BufferedReader itemBfr = new BufferedReader(new InputStreamReader(itemConn.getInputStream()));
        return itemBfr;
    }

    public static BufferedReader readFromFile(String filePath) throws IOException {
        //return Buffered Reader to read from file
        FileReader fr = new FileReader(filePath);
        BufferedReader bfr = new BufferedReader(fr);
        return bfr;
    }

    public static void writeToFile(String content, String filePath, int lastId){
        try {
            FileWriter fw = new FileWriter(filePath, true);
            fw.write(content);
            fw.close();
            FileWriter configFile = new FileWriter("config\\state.txt");
            configFile.write("id=" + lastId);
            configFile.close();

        }catch (IOException ioEx) {
            System.out.println(ioEx.toString());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //initialize app
        initApp();
        // URl object url, call gw2 api for item id list.
        File itemDbFile = new File(configList.get("ITEM_DB_PATH"));
        itemDbFile.delete();
        String itemDbHeader = "ID,NAME,LEVEL,ICON,CHAT_LINK\n";
        writeToFile(itemDbHeader, configList.get("ITEM_DB_PATH"), -1);
        BufferedReader bfr = getApiResponse(configList.get("API_ENTRY"));
        String itemId;
        String itemIdContainer = "";
        int idCount = 0;
        JSONArray itemList = new JSONArray();
        // Loops all item ids.
        try {
            while ((itemId = bfr.readLine()) != null) {
                if (itemId.indexOf("[") == -1 && itemId.indexOf("]") == -1) {
                    itemId = itemId.trim();
                    if (itemId.indexOf(",") != -1) {
                        // Every itemId that has a comma
                        itemId = itemId.replace(",", "");
                    }
                    if (idCount == 0) {
                        itemIdContainer = itemIdContainer + itemId;
                    } else {
                        itemIdContainer = itemIdContainer + "," + itemId;
                    }
                    idCount++;
                    numItems++;
                    // Gets 200 IDs and appends to the URL
                    if (idCount == Integer.parseInt(configList.get("MAX_PULL_IDS_COUNT"))) {
                        String itemUrlString = configList.get("API_ENTRY") + configList.get("API_ITEM_SUFFIX") + itemIdContainer;

                        // Call gw2 API for 200 items.
                        BufferedReader itemBuffer = getApiResponse(itemUrlString);
                        String itemLine;
                        StringBuffer itemSb = new StringBuffer();

                        // Read found items into a item list.
                        while ((itemLine = itemBuffer.readLine()) != null) {
                            itemSb.append(itemLine);
                        }
                        JSONArray apiToItemList = new JSONArray(itemSb.toString());
                        String fileContent = "";
                        for (int i = 0; i < apiToItemList.length(); i++) {
                            JSONObject item = apiToItemList.getJSONObject(i);
                            itemList.put(item);

                            System.out.println("ID: " + item.get("id") + ", name: " + item.get("name"));
                            fileContent = fileContent + item.get("id") + ","
                                                      + item.get("name") + ","
                                                      + item.get("level") + ",";
                            if(item.has("icon")){
                                fileContent = fileContent +  item.get("icon") + ",";
                            }
                            fileContent = fileContent +  item.get("chat_link") + "\n";

                            fileContent = fileContent.replace("(", "");
                            fileContent = fileContent.replace(")", "");
                        }

                        int index = itemList.length() - 1;
                        int lastItemId = itemList.getJSONObject(index).getInt("id");
                        writeToFile(fileContent, configList.get("ITEM_DB_PATH"), lastItemId);
                        idCount = 0;
                        itemIdContainer = "";
                        itemSb.delete(0, itemSb.length());
                        System.out.println("Sleeping, " + "Number of items: " + numItems);
                        Thread.sleep(Integer.parseInt(configList.get("API_STANDARD_WAIT_MS")));
                        if (numItems % 8000 == 0) {
                            System.out.println("Sleeping longer....");
                            Thread.sleep(Integer.parseInt(configList.get("API_LONG_WAIT_MS")));

                        }
                    }
                }
            }
            // Premature EOF Exception
        }catch (IOException e){
            System.out.println(e.toString());
        }
    }
}
