import asyncio
import logging
import random

import aiohttp
import os

from websearch.base_search import DDGS

# 设置日志
logging.basicConfig(level=logging.ERROR)
log = logging.getLogger(__name__)

import logging

try:
    from bs4 import BeautifulSoup
except ImportError:
    raise ImportError(
        "Webpage requires extra dependencies. Install with `pip install beautifulsoup4==4.12.3`"
    ) from None

logger = logging.getLogger(__name__)


def get_google_news(query_word):
    base_url = "https://www.google.com.hk/search"

    # 构建查询参数
    query_params = {
        "q": query_word.replace(" ", "+"),
        "tbm": "nws",
        "tbs": "sbd:1"
    }
    resp = DDGS(proxy="127.0.0.1:7890").get_google_url("GET", base_url, params=query_params)
    # 解析HTML内容
    soup = BeautifulSoup(resp, "html.parser")
    # 初始化结果字典
    results = {}
    news_divs = soup.find_all('div', class_='SoaBEf')
    result = []
    # 遍历每个div块并提取信息
    for div in news_divs:
        # 提取新闻标题
        title = div.find('div', class_='n0jPhd').text.strip()
        # 提取新闻URL
        url = div.find('a', class_='WlydOe')['href']
        # 提取新闻描述
        description = div.find('div', class_='GI74Re').text.strip()
        # 提取新闻来源网站名
        source = div.find('div', class_='MgUUmf').text.strip()
        # 提取新闻时间
        time = div.find('div', class_='OSrXXb').text.strip()
        news_info = {
            "新闻标题": title,
            "新闻URL": url,
            "新闻描述": description,
            "新闻来源": source,
            "新闻时间": time
        }
        print(news_info)
        # 将字典添加到result列表中
        result.append(news_info)
    return result


def get_baidu_news(word):
    payload = {
        "word": word,
        # "rtt": "1",#焦点排序
        "rtt": "4",#时间排序
        "tn": "news",
        "pn": "0",
    }
    resp_content = DDGS(proxy="127.0.0.1:7890").get_baidu_url("GET", "http://www.baidu.com/s", params=payload)
    soup = BeautifulSoup(resp_content, 'html.parser')

    # 查找所有包含新闻信息的div块
    news_divs = soup.find_all('div', class_='result-op c-container xpath-log new-pmd')
    result = []
    # 遍历每个div块并提取信息
    for div in news_divs:
        title = div.find('h3', class_='news-title_1YtI1').get_text(strip=True)
        url = div.find('h3', class_='news-title_1YtI1').a['href']
        description = div.find('span', class_='c-font-normal c-color-text').get_text(strip=True)
        source = div.find('span', class_='c-color-gray').get_text(strip=True)
        try:
            time = div.find('span', class_='c-color-gray2').get_text(strip=True)
        except Exception:
            time = None

        # 将信息组合成一个字典
        news_info = {
            "新闻标题": title,
            "新闻URL": url,
            "新闻描述": description,
            "新闻来源": source,
            "新闻时间": time
        }
        print(news_info)
        # 将字典添加到result列表中
        result.append(news_info)
    return result


def get_baidu(word):
    try:
        payload = {
            "wd": word,
            "pn": "0",
        }
        resp_content2 = DDGS().get_baidu_url("GET", "http://www.baidu.com/s", params=payload)
        soup2 = BeautifulSoup(resp_content2, 'html.parser')
        result = {}
        div_elements = soup2.find_all('div', class_='result c-container xpath-log new-pmd')

        for div_element in div_elements:
            # 获取 "mu" 属性值
            mu_link = div_element.get('mu')
            span_element = div_element.find('span', class_='content-right_1THTn')
            if not span_element:
                span_element = div_element.find('span', class_='content-right_2s-H4')
            if span_element:
                span_text = span_element.text
                result[mu_link] = span_text
            else:
                pass
        if result:
            return result
        else:
            return get_duckduckgo(word)
    except Exception as e:
        log.error("get_baidu error", e)
    return get_duckduckgo(word)


def get_duckduckgo(word):
    try:
        results = DDGS().text(word, region='cn-zh', max_results=10)
        return {item["href"]: item["body"] for item in results}
    except Exception as e:
        log.error("get_duckduckgo error", e)
    return None




def get_google(query_word, search_country="cn", search_language="zh-cn"):
    base_url = "https://www.google.com.hk/search"

    # 构建查询参数
    query_params = {"q": query_word.replace(" ", "+")}
    if search_country:
        query_params["gl"] = search_country
    if search_language:
        query_params["hl"] = search_language
    resp = DDGS(proxy="127.0.0.1:7890").get_google_url("GET", base_url, params=query_params)
    # 解析HTML内容
    soup = BeautifulSoup(resp, "html.parser")
    # 初始化结果字典
    results = {}
    for item in soup.select('div.MjjYud'):
        # 提取每个包含链接的div
        itemUrl = item.select_one('div.yuRUbf')
        # 获取href链接
        if not itemUrl:
            continue
        link_element = itemUrl.select_one('a[jsname="UWckNb"]')
        if link_element and link_element.has_attr('href'):
            href = link_element['href']

            # 获取文本内容
            text_element = item.select_one('div[style="-webkit-line-clamp:2"]')
            if text_element:
                text = text_element.get_text(strip=True)
            else:
                text = ""

            # 将链接和文本添加到结果字典
            results[href] = text
    return results


def get_pages_content(url_list):
    return asyncio.run(url_list_text(url_list))


async def url_list_text(url_list):
    tasks = [fetch_content(url) for url in url_list]
    results = await asyncio.gather(*tasks)
    # print(results)
    return results


from playwright.async_api import async_playwright

file_path = os.path.join(scriptPath, 'websearch', 'stealth.min.js')

# 使用绝对路径打开文件
with open(file_path, 'r') as f:
    js = f.read()
userAgent = [
    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.289 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0",
    "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.289 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36 SE 2.X MetaSr 1.0",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36 SLBrowser/9.0.3.1311 SLBChan/128",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.289 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36 SE 2.X MetaSr 1.0"
]


async def playwright_async_demo(url):
    try:
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True, args=[
                '--disable-blink-features=AutomationControlled',
                ('--user-agent=%s' % random.choice(userAgent))
            ])
            page = await browser.new_page()
            await page.add_init_script(js)
            await page.set_viewport_size({'width': 1024, 'height': 768})
            await page.goto(url)
            await page.wait_for_load_state("networkidle")
            content = await page.content()
            await browser.close()
            html = BeautifulSoup(content, 'html.parser')
            text = html.text.replace("\n", " ").strip()
            if any(text.startswith(s) for s in ["百度热搜", "百度安全验证", "会员登录", "安全验证"]):
                return None
            return text
    except Exception as e:
        print(f"Error occurred while processing {url}: {e}")
        return None


async def fetch_content(url):
    try:
        headers = {
            'User-Agent': random.choice(userAgent),
        }
        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers) as response:
                content = await response.text()
                html = BeautifulSoup(content, 'html.parser')
                text = html.get_text(separator=" ").replace("\n", " ").strip()
                if any(text.startswith(s) for s in ["百度热搜", "百度安全验证", "会员登录", "安全验证"]):
                    return None
                return text
    except Exception as e:
        print(f"Error occurred while processing {url}: {e}")
        return None
