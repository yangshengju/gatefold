package cn.gooday;

import cn.gooday.framework.dao.DatabaseHelper;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by yangshengju on 2017/11/20.
 */
public class ToolServiceImpl implements ToolService {

    public Logger logger = Logger.getLogger(ToolServiceImpl.class.getName());

    @Override
    public Map<String,String> downLoadGateFold(String targetDownloadAddr){
        HashMap<String,String> result = new HashMap<>();
        List<Map<String,Object>> needToDownLoadMapList = getDownloadList();
        Set<String> insertValues = new HashSet<>();
        long startTime = new Date().getTime();
        logger.info("StartTime : "+startTime);
        logger.info("一共有"+needToDownLoadMapList.size()+"个产品需要处理");
        for(int i=0;i<needToDownLoadMapList.size();i++) {
            logger.info("当前为第"+i+"个产品！");
            Map<String,Object> sellProImgDesrMap = needToDownLoadMapList.get(i);
            String produCode = (String)sellProImgDesrMap.get("produCode");
            String gateFold = changeClob2Str(sellProImgDesrMap.get("descrContent"));
            try {
                downLoadToLocal(produCode, gateFold, targetDownloadAddr, insertValues);
            } catch(Exception e) {
                e.printStackTrace();
                logger.info("当前产品处理异常："+e.getMessage()+produCode);
            }
        }
        logger.info("start to insert produ code no image..");
        insertProduCodeNoGateFold(insertValues);
        logger.info("end to insert produ code no image..");
        long endTime =new Date().getTime();
        logger.info("EndTime : "+endTime);
        logger.info("duration : "+(endTime-startTime));
        return result;
    }

    /**
     * 插入产品编码
     * @param insertValues
     */
    private void insertProduCodeNoGateFold(Set<String> insertValues) {
        //1、TRUNCATE TABLE produ_code_no_gatefold
        DatabaseHelper.update("truncate table produ_code_no_gatefold");
        //2、插入数据
        for(String produCode : insertValues) {
            DatabaseHelper.update("insert into produ_code_no_gatefold(produ_code) values(?)",produCode);
        }
    }

    /**
     * 下载图片到指定文件夹
     * @param produCode
     * @param gateFold
     * @param downloadDir
     */
    private void downLoadToLocal(String produCode, String gateFold, String downloadDir,Set<String> insertValues) {
        DOMParser parser = new DOMParser();

            try {
                parser.parse(new InputSource(new StringReader(gateFold)));
            } catch (SAXException e) {
                e.printStackTrace();
                logger.info("解析HTML内容异常："+produCode);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("解析HTML内容异常："+produCode);
                return;
            }
            Document document=parser.getDocument();
            if(document!=null&&document.getElementsByTagName("img")!=null) {
                NodeList nodeList = document.getElementsByTagName("img");
                if(nodeList!=null&&nodeList.getLength()>0) {
                    for (int j = 0; j < nodeList.getLength(); j++) {
                        Element node = (Element) nodeList.item(j);
                        File file = null;
                        try {
                            // 统一资源
                            URL url = new URL(node.getAttribute("src"));
                            // http的连接类
                            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                            httpURLConnection.setConnectTimeout(15000);
                            httpURLConnection.setReadTimeout(15000);
                            // 设定请求的方法，默认是GET
//                        httpURLConnection.setRequestMethod("POST");
                            // 设置字符编码
                            httpURLConnection.setRequestProperty("Charset", "UTF-8");
                            // 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
                            httpURLConnection.connect();

                            // 文件大小
                            int fileLength = httpURLConnection.getContentLength();

                            // 文件名
                            String filePathUrl = httpURLConnection.getURL().getFile();
                            String suffix = filePathUrl.substring(filePathUrl.lastIndexOf("."));
                            BufferedInputStream bin = new BufferedInputStream(httpURLConnection.getInputStream());
                            String path = "";
                            if (j > 0) {
                                path = downloadDir + File.separatorChar + produCode + "_" + j + suffix;
                            } else {
                                path = downloadDir + File.separatorChar + produCode + suffix;
                            }
                            file = new File(path);
                            if (!file.getParentFile().exists()) {
                                file.getParentFile().mkdirs();
                            }
                            OutputStream out = new FileOutputStream(file);
                            int size = 0;
                            int len = 0;
                            byte[] buf = new byte[1024];
                            while ((size = bin.read(buf)) != -1) {
                                len += size;
                                out.write(buf, 0, size);
                            }
                            bin.close();
                            out.close();
                        } catch (MalformedURLException e) {

                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } finally {
//                         return file;
                        }
                        System.out.println(node.getAttribute("src"));
                    }
                } else {
                    insertValues.add(produCode);
                }
            } else {
                //不包含img标签，则表示只包含文字
                insertValues.add(produCode);
            }
//        }


    }

    /**
     * 获取产品拉页信息
     * @return
     */
    private List<Map<String,Object>> getDownloadList() {
        List<Map<String,Object>> needToDownloadList = new ArrayList<>();
        needToDownloadList=DatabaseHelper.queryMapList("SELECT p.produ_code as produCode, d.descr_content as descrContent\n" +
                "          FROM bs_product p, sell_pro_image_description d\n" +
                "         WHERE p.produ_id = d.produ_id\n" +
                "         AND shop_id=394\n" +
                "         AND trim(p.produ_code) is not null\n" +
                "         AND trim(d.descr_content) is not null");
        return needToDownloadList;
    }


    /**
     * 将clob字段转换成String类型，便于后续处理
     * @param clobData
     * @return
     */
   private String changeClob2Str(Object clobData) {
        if(clobData!=null) {
            try {
                String reStr = ((Clob) clobData).getSubString(1, (int)((Clob) clobData).length());
                return reStr;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}
