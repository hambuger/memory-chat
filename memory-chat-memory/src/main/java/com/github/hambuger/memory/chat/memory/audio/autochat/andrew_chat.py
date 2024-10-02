import os
import time
from concurrent.futures import ThreadPoolExecutor

from chat_gpt import ChatClass
from audio_kws import get_audio
executor = ThreadPoolExecutor(10)
input_str = None

# Records the last time a valid input was detected
last_input_time = 0
# Whether voice chat is activated
audio_active = False
# Default audio file path
file_path = 'C:\\Users\\Administrator\\IdeaProjects\\memory-chat\\temp\\audio.wav'
# Record the ID of the last dialog response message
parent_id = '0'
chatClass = ChatClass()


print("Andrew chat started")
while True:
    try:
        audio_text = None
        if get_audio(audio_active, file_path):
            # Execute ASR and print the result
            audio_text =chatClass.audio_to_text(file_path)
            print(f"Recognize speech：{audio_text}")
            last_input_time = time.time()
            audio_active = True
        if not audio_text and not input_str:
            continue
        bye_word = os.getenv('BYE_WORD', 'goodbye')
        if audio_text and bye_word in audio_text.lower():
            audio_active = False
            parent_id = '0'
            chatClass.tts_pyttsx3(bye_word)
            print('\033[32m' + "goodbye!" + '\033[0m')
            continue
        audio_text = (f"""{input_str}\n{audio_text}""" if audio_text else input_str) if input_str else audio_text
        if audio_text != input_str:
            print('\033[32m' + f"{os.getenv('MY_NAME')}：{audio_text}" + '\033[0m')
        answer = chatClass.chat(audio_text)
        if not answer:
            continue
        print('\033[31m' + f"AI response：{answer}" + '\033[0m')
        chatClass.tts_pyttsx3(answer)
        last_input_time = time.time()
    except Exception as e:
        print(e)
        continue
