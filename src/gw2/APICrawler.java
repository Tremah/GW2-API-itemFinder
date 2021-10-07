package gw2;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.*;


public class APICrawler {
    private final static String API_ENTRY = "https://api.guildwars2.com/v2/items";
    private final static String API_ITEM_SUFFIX = "?ids=";
    private final static int SLEEP_AMOUNT = 500;
    private final static int MAX_ID_COUNT = 200;
    private static int numItems = 0;
    private static String itemDbPath = "C:\\Users\\Warren\\IdeaProjects\\Project_Tremah\\db\\db.csv";
    public static BufferedReader getApiResponse(String url) throws IOException {

        URL itemUrl = new URL(url);
        HttpURLConnection itemConn = (HttpURLConnection) itemUrl.openConnection();
        itemConn.setRequestMethod("GET");
        itemConn.setRequestProperty("Content-Type", "application/json");
        BufferedReader itemBfr = new BufferedReader(new InputStreamReader(itemConn.getInputStream()));
        return itemBfr;
    }

    public static void writeToFile(String content, String filePath, int lastId){
        try {
            FileWriter fw = new FileWriter(filePath, true);
            fw.write(content);
            fw.close();
            FileWriter configFile = new FileWriter("C:\\Users\\Warren\\IdeaProjects\\Project_Tremah\\config\\state.txt");
            configFile.write("id=" + lastId);
            configFile.close();

        }catch (IOException ioEx) {
            System.out.println(ioEx.toString());
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        // URl object url, call gw2 api for item id list.
        File itemDbFile = new File(itemDbPath);
        itemDbFile.delete();
        String itemDbHeader = "ID,NAME,LEVEL,ICON,CHAT_LINK\n";
        writeToFile(itemDbHeader, itemDbPath, -1);
        BufferedReader bfr = getApiResponse(API_ENTRY);
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
                    // Gets 200 ID and appends to the URL
                    if (idCount == MAX_ID_COUNT) {
                        String itemUrlString = API_ENTRY + API_ITEM_SUFFIX + itemIdContainer;

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
                        writeToFile(fileContent, itemDbPath, lastItemId);
                        idCount = 0;
                        itemIdContainer = "";
                        itemSb.delete(0, itemSb.length());
                        System.out.println("Sleeping, " + "Number of items: " + numItems);
                        Thread.sleep(SLEEP_AMOUNT);
                        if (numItems % 8000 == 0) {
                            System.out.println("Sleeping longer....");
                            Thread.sleep(1000 * 3);

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
/*
        for(int i = 0; i < array.length(); i++){
        JSONObject object = array.getJSONObject(i);
        System.out.println(object.getString("Name"));
        //JSONObject jo = new JSONObject((itemLine));
        //System.out.println(itemSb.toString());
        }
*/