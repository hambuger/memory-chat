import re
from io import BytesIO

import langid
import pyttsx3
import requests
from openai import OpenAI
from zhon import hanzi

client = OpenAI(api_key="sk-xxx")
messages = []


class ChatClass:
    def __init__(self):
        url = "http://localhost:8080/chat/send/register"  # 替换为实际的服务器地址
        params = {
            "channelEnum": "AUDIO",  # 替换为实际的channelEnum值
            "registerUrl": "http://localhost:5000/tts"  # 替换为实际的registerUrl值
        }
        requests.get(url, params=params)

    def tts_pyttsx3(text, rate=150, volume=1.0, voice_index=0):
        """
        使用pyttsx3将文本转为语音并播放
        :param text: 要朗读的文本
        :param rate: 语速，默认150
        :param volume: 音量，范围为0.0到1.0，默认1.0
        :param voice_index: 选择语音索引，默认0为男性，1为女性
        """
        # 初始化TTS引擎
        engine = pyttsx3.init()

        # 设置语速
        engine.setProperty('rate', rate)

        # 设置音量
        engine.setProperty('volume', volume)

        # 设置语音
        voices = engine.getProperty('voices')
        if voice_index < len(voices):
            engine.setProperty('voice', voices[voice_index].id)
        else:
            print(f"指定的语音索引 {voice_index} 超出范围，使用默认语音。")

        # 朗读文本
        engine.say(text)

        # 等待朗读完成
        engine.runAndWait()

    def audio_to_text(file):
        audio_file = open(file, "rb")
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
        return transcript.text

    def transcribe_audio(audio):
        wav_data = BytesIO(audio.get_wav_data())
        with open("speech.mp3", "wb") as f:
            f.write(wav_data.read())
        audio_file = open("speech.mp3", "rb")
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
        return transcript.text

    def chat(prompt):
        global messages
        messages.append({"role": "user", "content": prompt})
        # completion = client.chat.completions.create(
        #     model="gpt-4o",
        #     messages=messages
        # )
        # print(completion.choices[0].message)
        # answer = completion.choices[0].message.content
        # messages.append({"role": "assistant", "content": answer})

        url = 'http://127.0.0.1:8080/chat/message'  # 将此 URL 替换为实际的 API 端点

        # 定义请求的参数
        data = {
            'messageContent': prompt,
            'messageContentType': 'TEXT',
            'messageCreatorName': '小明',
            'groupMsgFlag': '0',
            'realCreatorId': '小明',
            'realCreatorName': '小明',
            'channelEnum': 'AUDIO',
            'receiveMessageUserId': 'xiaoming'
        }

        response = requests.post(url, json=data)
        answer = ''
        for msg in response.json().get('sendMessageList'):
            messages.append({"role": "assistant", "content": msg.get('messageContent')})
            answer = answer + msg.get('messageContent')
        return answer

    # 定义一个函数来检测文本是否具有中文语言意义
    def has_chinese_meaning(text):
        # 检测文本是否包含中文句子模式
        if re.search(hanzi.sentence, text):
            return True
        lang, confidence = langid.classify(text)
        if lang == 'zh':
            return True
        # 如果都没有，则返回False
        return False
