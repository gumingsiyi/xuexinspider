import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.gson.Gson;
import fateadm.Api;
import fateadm.Util;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XuexinSpider {
    private WebClient client;
    private String token;
    private Api api;
    private Util.HttpResp resp;

    public XuexinSpider() {
        client = new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        String app_id = "305014";
        String app_key = "5ApuPty6FhuhWpk0tos5hzvCoqsOrWTY";
        String pd_id = "105014";
        String pd_key = "Xgani5VEkQdpeZd+pmp0ZS2sWjaSVmZv";
        api = new Api();
        api.Init(app_id, app_key, pd_id, pd_key);
    }
    public static void main(String[] args) throws Exception {
        XuexinSpider spider = new XuexinSpider();
        //String token = spider.getPhoneToken();
        //String phn = spider.getPhoneNum();
        //String res = spider.getJsonInfo(null);
        String res = spider.get("110771200905001323", "王海全");
        //String res = spider.get("126611201005000148", "黄攀");
        //String res = spider.get("132491201605000515", "陈丽");

        System.out.println(res);
        //spider.ReleasePhone();
    }
    public String get(String num, String name) throws Exception {
        HtmlPage pageVcode = getPageVcode(num, name);
        String url = pageVcode.getUrl().toString();
        System.out.println(pageVcode.getUrl());
        String rndid = getRndid(url);
        while (rndid == null) {
            System.out.println("验证码识别识别失败, 重试中...");
            resp = api.Justice(resp.req_id);
            pageVcode = getPageVcode(num, name);
            url = pageVcode.getUrl().toString();
            rndid = getRndid(url);
            System.out.println(pageVcode.getUrl());
        }
        System.out.println(rndid);
        HtmlPage resPage = getResPage(pageVcode, rndid);
        return getJsonInfo(resPage);
    }

    private HtmlPage getPageVcode(String num, String name) throws Exception {
        HtmlPage page = client.getPage("https://www.chsi.com.cn/xlcx/lscx/query.do");
        HtmlInput zsbhInput = (HtmlInput)page.getElementById("zsbh");
        zsbhInput.setValueAttribute(num);
        HtmlInput nameInput = (HtmlInput)page.getElementById("xm");
        nameInput.setValueAttribute(name);
        HtmlImage img = (HtmlImage)page.getElementById("captchImage");
        img.saveAs(new File("xxwyzm.jpg"));

        String yzm = getImgVCode();
        HtmlInput yzmInput = (HtmlInput)page.getElementById("yzm");
        yzmInput.setValueAttribute(yzm);
        HtmlInput btn = (HtmlInput)page.getElementById("xueliSubmit");
        return btn.click();
    }

    private String getRndid(String url) {
        Pattern pattern = Pattern.compile("rndid=(.*?)&");
        Matcher m = pattern.matcher(url);
        if (m.find()){
            return m.group(1);
        }
        return null;
    }

    private HtmlPage getResPage(HtmlPage page, String rndid) throws Exception {
        HtmlInput phnInput = (HtmlInput)page.getElementById("mphone");
        String token = getPhoneToken();
        while (token == null) {
            System.out.println("手机验证码token获取识别，重试中...");
            Thread.sleep(1000);
            token = getPhoneToken();
        }
        String phn = getPhoneNum();
        while (phn == null || "".equals(phn)) {
            System.out.println("手机号码申请失败，重试中...");
            Thread.sleep(1000);
            phn = getPhoneNum();
        }

        phnInput.setValueAttribute(phn);
        HtmlInput vcodeInput = (HtmlInput)page.getElementById("vcode");
        WebRequest post = new WebRequest(new URL("https://www.chsi.com.cn/xlcx/lscx/sendvcode.do"), HttpMethod.POST);
        ArrayList<NameValuePair> list = new ArrayList<>();
        list.add(new NameValuePair("mphone", phn));
        list.add(new NameValuePair("rndid", rndid));
        post.setRequestParameters(list);
        client.getPage(post);

        String vcode = getVCode(phn);
        while (vcode == null) {
            Thread.sleep(1000);
            vcode = getVCode(phn);
        }
        vcodeInput.setValueAttribute(vcode);
        HtmlInput subBtn = (HtmlInput)page.getElementById("newbutton");
        return subBtn.click();
    }

    private String getJsonInfo(HtmlPage page) throws Exception {
        //HtmlPage page = client.getPage("https://www.chsi.com.cn/xlcx/lscx/mobileval.do?rndid=jsad12s83mbnaosccnplh8vb9h4fzw5j&state=CHSI");
        List<DomElement> list = page.getElementsByTagName("tr");
        if (list.size() < 8) {
            System.out.println("短信验证码获取错误，或者查无此人，请重试...");
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < 8; i++) {
            DomElement element = list.get(i);
            List<HtmlElement> keyList = element.getElementsByTagName("th");
            List<HtmlElement> valueList = element.getElementsByTagName("td");
            map.put(keyList.get(0).asText().replace("：", ""), valueList.get(0).asText());
            map.put(keyList.get(1).asText().replace("：", ""), valueList.get(1).asText());
        }
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    private String getPhoneToken() {
        try {
            WebRequest get = new WebRequest(new URL("http://kapi.yika66.com:20153/User/login?uName=yyq&pWord=258369&Developer=9sx2SDqYbZSChGrAMR0oJw%3d%3d"), HttpMethod.GET);
            HtmlPage page = client.getPage(get);

            String[] res = page.asText().split("&");
            if (res.length < 2) {
                return null;
            } else {
                token = res[0];
                return res[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPhoneNum() {
        try {
            WebRequest get = new WebRequest(new URL("http://kapi.yika66.com:20153/User/getPhone?ItemId=488&token=" + token), HttpMethod.GET);
            HtmlPage page = client.getPage(get);
            String[] res = page.asText().split(";");
            return res[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getVCode(String phn) {
        try {
            WebRequest get = new WebRequest(new URL("http://kapi.yika66.com:20153/User/getMessage?token="+token+"&ItemId=488&Phone="+phn), HttpMethod.GET);
            HtmlPage page = client.getPage(get);
            String msg = page.asText();
            //"MSG&488&19809404479&【学信网】学历查询短信验证码：610526，有效期15分钟，本条信息免费。[End]NOTION&API接口已经升级更新,平台稳定,号码充足[End]"
            String[] list = msg.split("&");
            if (list.length < 4) {
                return null;
            }
            msg = list[3];
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < msg.length(); i++) {
                if (res.length() == 6) {
                    return new String(res);
                }
                if (Character.isDigit(msg.charAt(i))) {
                    res.append(msg.charAt(i));
                } else {
                    res = new StringBuilder();
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getImgVCode() throws Exception {
        String pred_type = "80300";
        String img_file = "./xxwyzm.jpg";
        resp = api.PredictFromFile(pred_type, img_file);
        System.out.println("Image code is " + resp.pred_resl);
        return resp.pred_resl;
    }
}
