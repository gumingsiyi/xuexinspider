import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.gson.Gson;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XuexinSpider {
    private WebClient client;

    public XuexinSpider() {
        client = new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
    }
    public static void main(String[] args) throws Exception {
        XuexinSpider spider = new XuexinSpider();
        String res = spider.get("110771200905001323", "王海全");
        System.out.println(res);
    }
    public String get(String num, String name) throws Exception {
        HtmlPage pageVcode = getPageVcode(num, name);
        String url = pageVcode.getUrl().toString();
        System.out.println(pageVcode.getUrl());
        String rndid = getRndid(url);
        while (rndid == null) {
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
        Scanner scan = new Scanner(System.in);
        //TODO 调用获取验证码
        String yzm = scan.nextLine();
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
        //TODO 设置接收号码的手机
        phnInput.setValueAttribute("17306424123");
        HtmlInput vcodeInput = (HtmlInput)page.getElementById("vcode");
        WebRequest post = new WebRequest(new URL("https://www.chsi.com.cn/xlcx/lscx/sendvcode.do"), HttpMethod.POST);
        ArrayList<NameValuePair> list = new ArrayList<>();
        list.add(new NameValuePair("mphone", "17306424123"));
        Scanner scan = new Scanner(System.in);
        list.add(new NameValuePair("rndid", rndid));
        post.setRequestParameters(list);
        client.getPage(post);
        //TODO 接收到的验证码
        String msg = scan.nextLine();
        vcodeInput.setValueAttribute(msg);
        HtmlInput subBtn = (HtmlInput)page.getElementById("newbutton");
        return subBtn.click();
    }

    private String getJsonInfo(HtmlPage page) throws Exception {
        //HtmlPage page = client.getPage("https://www.chsi.com.cn/xlcx/lscx/mobileval.do?rndid=jsad12s83mbnaosccnplh8vb9h4fzw5j&state=CHSI");
        List<DomElement> list = page.getElementsByTagName("tr");
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
}
