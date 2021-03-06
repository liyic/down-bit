package com.wdbyte.downbit.thread;

import com.wdbyte.downbit.DownloadMain;
import com.wdbyte.downbit.util.FileUtils;
import com.wdbyte.downbit.util.HttpUtls;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

/**
 * <p>
 * 多线程下载工具类
 *
 * @author niujinpeng
 * @Date 2020/7/19 21:26
 */
public class DownloadThread implements Callable<Boolean> {

    // 每次读取的数据块大小
    private static int BYTE_SIZE = 1024 * 100;
    // 下载链接
    private String url;
    // 下载开始位置
    private long startPos;
    // 要下载的文件区块大小
    private Long endPos;
    // 标识多线程下载切分的第几部分
    private Integer part;

    public DownloadThread(String url, long startPos, long endPos, Integer part) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
    }

    @Override
    public Boolean call() throws Exception {
        if (url == null || url.trim() == "") {
            throw new RuntimeException("> 下载路径不正确");
        }
        // 文件名
        String httpFileName = HttpUtls.getHttpFileName(url);
        if (part != null) {
            httpFileName = httpFileName + DownloadMain.FILE_TEMP_SUFFIX + part;
        }
        // 本地文件大小
        Long localFileContentLength = FileUtils.getFileContentLength(httpFileName);
        if (localFileContentLength >= endPos - startPos) {
            System.out.println("> " + httpFileName + " 已经下载完毕，无需重复下载");
            return true;
        }
        HttpURLConnection httpUrlConnection = HttpUtls.getHttpUrlConnection(url, startPos + localFileContentLength, endPos);

        // 获得输入流
        try (InputStream input = httpUrlConnection.getInputStream(); BufferedInputStream bis = new BufferedInputStream(input);
             RandomAccessFile oSavedFile = new RandomAccessFile(httpFileName, "rw")) {
            // 文件写入开始位置 localFileContentLength
            oSavedFile.seek(localFileContentLength);
            LogThread.DOWNLOAD_SIZE.addAndGet(localFileContentLength);
            byte[] buffer = new byte[BYTE_SIZE]; // 数组大小可根据文件大小设置a
            int len = -1;
            while ((len = bis.read(buffer)) != -1) { // 读到文件末尾则返回-1
                oSavedFile.write(buffer, 0, len);
                LogThread.DOWNLOAD_SIZE.addAndGet(len);
            }
        } catch (FileNotFoundException e) {
            System.out.println("\n> ERROR! 要下载的文件路径不存在 " + url);
            return false;
        } catch (Exception e) {
            System.out.println("\n> 下载出现异常");
            e.printStackTrace();
            return false;
        } finally {
            httpUrlConnection.disconnect();
            LogThread.DOWNLOAD_FINISH.addAndGet(1);
        }
        return true;
    }

}
