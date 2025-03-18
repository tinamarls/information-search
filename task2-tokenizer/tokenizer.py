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

tokens_file = 'tokens.txt'
lemmatized_file = 'lemmatized_tokens.txt'

morph = MorphAnalyzer()
stop_words = set(stopwords.words('russian'))

def extract_text_from_html(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        soup = BeautifulSoup(file, 'html.parser')
        return soup.get_text()

# Функция для токенизации и фильтрации текста
def tokenize_and_filter(text):
    tokens = wordpunct_tokenize(text.lower())  # Приводим все к нижнему регистру для единообразия
    filtered_tokens = []

    for token in tokens:
        if not re.match(r'^[а-яА-ЯёЁ]+$', token):
            continue

        if token in stop_words:
            continue

        filtered_tokens.append(token)

    return filtered_tokens

# Функция для лемматизации и группировки токенов по леммам
def lemmatize_and_group(tokens):
    lemmatized_tokens = defaultdict(list)

    for token in tokens:
        lemma = morph.parse(token)[0].normal_form
        lemmatized_tokens[lemma].append(token)

    return lemmatized_tokens

def write_tokens_to_file(tokens, file_name):
    with open(file_name, 'w', encoding='utf-8') as f:
        for token in tokens:
            f.write(f"{token}\n")

# Функция для записи лемматизированных токенов в файл
def write_lemmatized_tokens_to_file(lemmatized_tokens, file_name):
    with open(file_name, 'w', encoding='utf-8') as f:
        for lemma, tokens in lemmatized_tokens.items():
            f.write(f"{lemma} {' '.join(tokens)}\n")

def process_documents():
    all_tokens = set()

    for filename in os.listdir(html_directory):
        if filename.endswith(".html"):
            file_path = os.path.join(html_directory, filename)
            text = extract_text_from_html(file_path)

            tokens = tokenize_and_filter(text)
            all_tokens.update(tokens)

    write_tokens_to_file(all_tokens, tokens_file)

    lemmatized_tokens = lemmatize_and_group(all_tokens)

    write_lemmatized_tokens_to_file(lemmatized_tokens, lemmatized_file)

process_documents()

print("Обработка завершена. Результаты записаны в файлы 'tokens.txt' и 'lemmatized_tokens.txt'.")
