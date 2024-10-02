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
#     def __init__(self):
# #         url = "http://localhost:80/chat/send/register"
# #         params = {
# #             "channelEnum": "AUDIO",
# #             "registerUrl": "http://localhost:5000/tts"
# #         }
# #         requests.get(url, params=params)
#         pass

    def tts_pyttsx3(self, text, rate=150, volume=1.0, voice_index=0):
        """
        Use pyttsx3 to convert text to speech and play it
        :param text: The text to be read aloud
        :param rate: Speech rate, default 150
        :param volume: Volume, ranging from 0.0 to 1.0, default 1.0
        :param voice_index: Select Phonetic Index, and by default, 0 is male and 1 is female
        """
        # Initialize the TTS engine
        engine = pyttsx3.init()

        # Set the speaking rate
        engine.setProperty('rate', rate)

        # Set the volume
        engine.setProperty('volume', volume)

        # Set up your voice
        voices = engine.getProperty('voices')
        if voice_index < len(voices):
            engine.setProperty('voice', voices[voice_index].id)
        else:
            print(f"Specified speech index {voice_index} Out of range, use the default voice。")

        # Read the text aloud
        engine.say(text)

        # Wait for the reading to complete
        engine.runAndWait()

    def audio_to_text(self, file):
        audio_file = open(file, "rb")
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
        return transcript.text

    def transcribe_audio(self, audio):
        wav_data = BytesIO(audio.get_wav_data())
        with open("speech.mp3", "wb") as f:
            f.write(wav_data.read())
        audio_file = open("speech.mp3", "rb")
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
        return transcript.text

    def chat(self, prompt):
        global messages
        messages.append({"role": "user", "content": prompt})
        # completion = client.chat.completions.create(
        #     model="gpt-4o",
        #     messages=messages
        # )
        # print(completion.choices[0].message)
        # answer = completion.choices[0].message.content
        # messages.append({"role": "assistant", "content": answer})

        url = 'http://127.0.0.1:80/chat/message'  # Replace this URL with the actual API endpoint

        # Define the parameters of the request
        data = {
            'messageContent': prompt,
            'messageContentType': 'TEXT',
            'messageCreatorName': 'Leonard',
            'groupMsgFlag': '0',
            'realCreatorId': 'Leonard',
            'realCreatorName': 'Leonard',
            'channelEnum': 'AUDIO',
            'receiveMessageUserId': 'Leonard'
        }

        response = requests.post(url, json=data)
        answer = ''
        for msg in response.json().get('sendMessageList'):
            messages.append({"role": "assistant", "content": msg.get('messageContent')})
            answer = answer + msg.get('messageContent')
        return answer

    # Define a function to detect whether text has Chinese linguistic meaning
    def has_chinese_meaning(self, text):
        # Detects whether the text contains Chinese sentence patterns
        if re.search(hanzi.sentence, text):
            return True
        lang, confidence = langid.classify(text)
        if lang == 'zh':
            return True
        # If there are none, False is returned
        return False
