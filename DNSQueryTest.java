package cn.creditease.selenium;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class DNSQueryTest {
	
	static RemoteWebDriver webkaka,chinaz;
	
	static {
		try {
			DesiredCapabilities desiredCapabilities = DesiredCapabilities.firefox();
			desiredCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
			desiredCapabilities.setCapability("ignoreProtectedModeSettings", true);
			webkaka = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"),desiredCapabilities);
			webkaka.get("http://www.webkaka.com/dns/");
			//设置DNS查询超时为9秒
			WebElement inputOverTime = webkaka.findElementById("inputOverTime");
			inputOverTime.clear();
			inputOverTime.sendKeys("9");
			
			chinaz = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"),desiredCapabilities);
			chinaz.get("http://tool.chinaz.com/dns/?type=1");
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		List<String> domain = new ArrayList<>();
		domain.add("consumeweb.alipay.com");
		String mitmIgnoreHosts = getMitmIgnoreHosts(domain);
		String cmd = "nohup mitmdump --listen-port 1723 --mode socks5 ";
		cmd += "--ignore-hosts '" + mitmIgnoreHosts + "' ";
		cmd += "-s main.py > socks5.log 2>&1 &";
		System.out.print(cmd);
		webkaka.quit();
		chinaz.quit();
	}
	
	
	private static String getMitmIgnoreHosts(List<String> domain) throws InterruptedException{
		String mitmIgnoreHosts = "^";
		for (String d : domain) {
			Set<String> result = queryWebkakaDomainDns(d);
			result.addAll(queryChinazDomainDns(d));
			for (String ip : result) {
				mitmIgnoreHosts += "(?!"+ip+")(?!"+d+")";
			}
		}
		return mitmIgnoreHosts;
	}
	
	private static Set<String> queryChinazDomainDns(String domain) throws InterruptedException {
		chinaz.get("http://tool.chinaz.com/dns/?type=1&host="+domain+"&ip=");
		Thread.sleep(10 * 1000);
		List<WebElement> dnsResults = chinaz.findElementsByCssSelector("div[class='w60-0 tl']");
		Set<String> result = new HashSet<>();
		for (WebElement dnsResult : dnsResults) {
			String w2text = dnsResult.getText();
			Pattern ipRegex = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
			Matcher matcher = ipRegex.matcher(w2text);
			while(matcher.find()) {
				result.add(matcher.group());
			}
		}
		System.out.println("chinaz"+result);
		return result;
	}
	
	private static Set<String> queryWebkakaDomainDns(String domain) throws InterruptedException {
		WebElement url1 = webkaka.findElementById("url1");
		url1.clear();
		url1.sendKeys(domain);
		WebElement btnDns1 = webkaka.findElementById("btnDns1");
		btnDns1.click();
		Thread.sleep(20 * 1000);
		List<WebElement> dnsResults = webkaka.findElementsByCssSelector("tr.DnsResultTableTr");
		List<WebElement> dnsResults2 = webkaka.findElementsByCssSelector("tr.DnsResultTableTrOdd");
		dnsResults.addAll(dnsResults2);
		Set<String> result = new HashSet<>();
		for (WebElement dnsResult : dnsResults) {
			WebElement w2 = dnsResult.findElement(By.cssSelector("td.w2"));
			String w2text = w2.getText();
			Pattern ipRegex = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
			Matcher matcher = ipRegex.matcher(w2text);
			while(matcher.find()) {
				result.add(matcher.group());
			}
		}
		return result;
	}
}
