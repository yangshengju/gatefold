package cn.gooday;

import java.util.Map;

/**
 * Created by yangshengju on 2017/11/20.
 */
public interface ToolService {
    /**
     * 下载产品拉页图片方法
     * @param targetDownloadAddr
     * @return
     */
    public Map<String,String> downLoadGateFold(String targetDownloadAddr);
}
