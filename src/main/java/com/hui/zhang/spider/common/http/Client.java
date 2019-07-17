package com.hui.zhang.spider.common.http;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

public class Client {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);
    private static Map<String,PoolingHttpClientConnectionManager> cmMap;
    private final static String KEY_STORE_FILE="client-edb.p12";
    private final static String KEY_STORE_PASS="edb123";
    private final static String TRUST_STORE_FILE="client-edb.jks";
    private final static String TRUST_STORE_PASS="edb123";
    //private final static String CERTIFICATE_PATH;
    private final static int MAX_CONN_TOTAL  = 500;   //最大链接数
    private final static int MAX_CONN_PER_ROUTE  = 500;//每个域名最大链接数

    static{
        cmMap=new HashMap<>();
    }
    /**
     * 获得httpclient
     * @return
     */
    public static CloseableHttpClient SSLClient(String url){
        String host=getHost(url);
        String hostHead=getHostHead(url);
        String cmKey=hostHead+host;

        PoolingHttpClientConnectionManager cm=cmMap.get(cmKey);
        if (null==cm){
            RegistryBuilder registryBuilder=RegistryBuilder.create();
            if (hostHead.equals("https")){
                String certificatePath="/certificate/"+host+"/";
                registryBuilder.register("https", getSSLConnectionSocketFactory(certificatePath));
            }
            registryBuilder.register("http", new PlainConnectionSocketFactory());
            Registry<ConnectionSocketFactory> socketFactoryRegistry = registryBuilder.build();
            cm =new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            cm.setMaxTotal(MAX_CONN_TOTAL);
            cm.setDefaultMaxPerRoute(MAX_CONN_PER_ROUTE);
            cmMap.put(cmKey,cm);
        }

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        return httpClient;
    }

    /**
     *获得 SSLConnectionSocketFactory
     * @return
     */
    private  static  SSLConnectionSocketFactory getSSLConnectionSocketFactory(String certificatePath){
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                getSslContext(certificatePath),
                new String[]{"TLSv1" },
                null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return  sslsf;
    }

    /**
     * 获得 SSLContext
     * @return
     */
    private static  SSLContext  getSslContext(String certificatePath){
        SSLContext sslContext=null;
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(getkeyStore(certificatePath),KEY_STORE_PASS.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();

            TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(getTrustStore(certificatePath));
            TrustManager[]  trustManagers= trustManagerFactory.getTrustManagers();


            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (FileNotFoundException e) {
            LOGGER.error("client-edb.p12  or client-edb.jks 文件找不到:{}",e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("没有该算法:{}",e);
        } catch (IOException e) {
            LOGGER.error("IO异常:{}",e);
        } catch (UnrecoverableKeyException e) {
            LOGGER.error("密码错误！:{}",e);
        } catch (KeyStoreException e) {
            LOGGER.error("密钥仓异常 ！:{}",e);
        } catch (KeyManagementException e) {
            LOGGER.error("密钥管理异常 ！:{}",e);
        }
        return  sslContext;

    }

    /**
     * 获得 KeyStore
     * @return
     * @throws IOException
     */
    private static KeyStore getkeyStore(String certificatePath) throws IOException{
        KeyStore keySotre=null;
        FileInputStream fis=null;
        try {  
            keySotre = KeyStore.getInstance("PKCS12");  
            fis = new FileInputStream(new File(Client.class.getResource("/").getPath()+certificatePath+KEY_STORE_FILE));
            keySotre.load(fis, KEY_STORE_PASS.toCharArray());
        } catch (KeyStoreException e) { 
        	LOGGER.error("密钥仓异常 ！:{}",e);
        } catch (FileNotFoundException e) {
        	LOGGER.error("client-edb.p12 文件找不到:{}",e);
        } catch (NoSuchAlgorithmException e) {
        	LOGGER.error("没有该算法:{}",e);
        } catch (CertificateException e) {
        	LOGGER.error("证书错误:{}",e);
        } catch (IOException e) {
        	LOGGER.error("IO异常:{}",e);
        }finally{
            if (null!=fis){
                fis.close();
            }
        }
        return keySotre;  
    }

    /**
     * 获得  getTrustStore
     * @return
     * @throws IOException
     */
    private static KeyStore getTrustStore(String certificatePath) throws IOException{
        KeyStore trustKeyStore=null;  
        FileInputStream fis=null;  
        try {  
            trustKeyStore=KeyStore.getInstance("JKS");  
            fis = new FileInputStream(new File(Client.class.getResource("/").getPath()+certificatePath+TRUST_STORE_FILE));
            trustKeyStore.load(fis, TRUST_STORE_PASS.toCharArray());  
        } catch (FileNotFoundException e) { 
        	LOGGER.error("client-edb.jks 文件找不到:{}",e);
        } catch (KeyStoreException e) {
        	LOGGER.error("密钥仓异常 ！:{}",e);
        } catch (NoSuchAlgorithmException e) {
        	LOGGER.error("没有该算法:{}",e);
        } catch (CertificateException e) {
        	LOGGER.error("证书错误:{}",e);
        } catch (IOException e) {
        	LOGGER.error("IO异常:{}",e);
        }finally{
            if (null!=fis){
                fis.close();
            }
        }
        return trustKeyStore;  
    }


    private static String getHost(String url){
        String host=url.substring(url.indexOf("://")+3,url.lastIndexOf("/"));
        return  host;
    }
    private static String getHostHead(String url){
        String hostHead=url.substring(0,url.indexOf("://"));
        return  hostHead;
    }

    public  static  void  main(String  argus[]){

    }

} 