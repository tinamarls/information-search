import os
import re
from bs4 import BeautifulSoup
from nltk.tokenize import wordpunct_tokenize
from nltk.corpus import stopwords
from pymorphy2 import MorphAnalyzer
from collections import defaultdict

import nltk
nltk.download('punkt')
nltk.download('stopwords')

html_directory = '/Users/kristina/IdeaProjects/information-search/task1-crawler/downloaded_pages'
tokens_output_directory = 'processed_tokens/tokens'
lemmatized_output_directory = 'processed_tokens/lemmatized'

os.makedirs(tokens_output_directory, exist_ok=True)
os.makedirs(lemmatized_output_directory, exist_ok=True)

morph = MorphAnalyzer()
stop_words = set(stopwords.words('russian'))

def extract_text_from_html(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        soup = BeautifulSoup(file, 'html.parser')
        return soup.get_text()

# Функция для токенизации и фильтрации текста
def tokenize_and_filter(text):
    tokens = wordpunct_tokenize(text.lower())
    return [token for token in tokens if re.match(r'^[а-яА-ЯёЁ]+$', token) and token not in stop_words]

# Функция для лемматизации и группировки токенов по леммам
def lemmatize_and_group(tokens):
    lemmatized_tokens = defaultdict(set)
    for token in tokens:
        lemma = morph.parse(token)[0].normal_form
        lemmatized_tokens[lemma].add(token)
    return lemmatized_tokens

# Функция для записи токенов в файл
def write_tokens_to_file(tokens, file_name):
    with open(file_name, 'w', encoding='utf-8') as f:
        f.write("\n".join(tokens))

# Функция для записи лемматизированных токенов в файл
def write_lemmatized_tokens_to_file(lemmatized_tokens, file_name):
    with open(file_name, 'w', encoding='utf-8') as f:
        for lemma, tokens in lemmatized_tokens.items():
            f.write(f"{lemma} {' '.join(tokens)}\n")

def process_documents():
    for filename in os.listdir(html_directory):
        if filename.endswith(".html"):
            file_path = os.path.join(html_directory, filename)
            text = extract_text_from_html(file_path)

            tokens = tokenize_and_filter(text)
            lemmatized_tokens = lemmatize_and_group(tokens)

            tokens_output_path = os.path.join(tokens_output_directory, f"tokens_{filename}.txt")
            lemmatized_output_path = os.path.join(lemmatized_output_directory, f"lemmatized_{filename}.txt")

            write_tokens_to_file(tokens, tokens_output_path)
            write_lemmatized_tokens_to_file(lemmatized_tokens, lemmatized_output_path)

            print(f"Обработан файл: {filename}")

process_documents()
print("Обработка завершена. Результаты записаны в папки 'processed_tokens/tokens' и 'processed_tokens/lemmatized'.")
