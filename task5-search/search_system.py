import collections
import natasha
import numpy
import os

# Используем относительные пути
TF_IDF_LEMMAS_PATH = '../task4-tf-idf/tf-idf-output/lemmas/'
INDEX_PATH = '../task1-crawler/index.txt'


def load_urls():
    urls = collections.defaultdict(str)

    with open(INDEX_PATH, 'r', encoding='utf-8') as file:
        lines = file.readlines()
        for line in lines:
            parts = line.split(' ')
            urls[int(parts[0])] = parts[1].replace('\n', '')

    return urls


def load_lemmas_tf_idf():
    lemmas_tf_idf = collections.defaultdict()

    for filename in os.listdir(TF_IDF_LEMMAS_PATH):
        file_path = TF_IDF_LEMMAS_PATH + filename
        index = ''

        for char in filename:
            if char.isdigit():
                index += char
        index = int(index)

        with open(file_path, 'r', encoding='utf-8') as file:
            lines = file.readlines()
            lemma_info = collections.defaultdict()

            # Заполняем словарь лемм и их соответствующих значений TF и IDF.
            for line in lines:
                parts = line.split(' ')
                lemma_info[parts[0]] = [float(parts[1].replace(',', '.')), float(parts[2].replace(',', '.'))]
            lemmas_tf_idf[index] = lemma_info

    return lemmas_tf_idf


# Функция для предобработки запроса и получения лемм.
def parse_request(request, segmenter, news_morph_tagger, morph_vocab):
    doc_request = natasha.Doc(request)
    doc_request.segment(segmenter)
    doc_request.tag_morph(news_morph_tagger)

    keys = []

    for token in doc_request.tokens:
        if token.pos in ['ADJ', 'NOUN', 'PRON', 'VERB']:
            token.lemmatize(morph_vocab)
            keys.append(token.lemma.lower())

    return keys


# Функция для подсчета частот лемм в запросе.
def parse_lemmas(keys, segmenter, news_morph_tagger, morph_vocab):
    lemmas = {}

    for i in range(0, len(keys)):
        doc = natasha.Doc(keys[i])
        doc.segment(segmenter)
        doc.tag_morph(news_morph_tagger)
        doc.tokens[0].lemmatize(morph_vocab)
        keys[i] = doc.tokens[0].lemma.lower()

        # Подсчитываем частоту каждой леммы.
        if keys[i] not in lemmas:
            lemmas[keys[i]] = 1
        else:
            lemmas[keys[i]] += 1

    return lemmas


# Функция для вычисления TF-IDF для лемм в запросе.
def calculate_tf_idf(keys, lemmas):
    tf_idf = {}

    for lemma in lemmas.keys():
        tf_idf[lemma] = lemmas[lemma] / len(keys)

    return tf_idf


# Функция для выполнения поиска вектора по запросу.
def vector_search(lemmas_tf_idf, tf_idf):
    found = {}

    for i in lemmas_tf_idf.keys():
        lemmas_info = lemmas_tf_idf[i]
        lemmas_tf = []
        request_tf = []

        # Собираем TF-IDF для лемм документа и запроса.
        for lemma in lemmas_info.keys():
            lemmas_tf.append(lemmas_info[lemma][0])
            request_tf.append(tf_idf[lemma] if lemma in tf_idf else 0.0)

        # рассчитываем косинусное сходство.
        if numpy.linalg.norm(request_tf) != 0:
            result = numpy.dot(lemmas_tf, request_tf) / (numpy.linalg.norm(lemmas_tf) * numpy.linalg.norm(request_tf))
            if result != 0:
                found[i] = float(result)  # Убираем np.float64

    sorted_found = sorted(found.items(), key=lambda item: item[1], reverse=True)

    return sorted_found


class SearchSystem:
    def __init__(self):
        self.segmenter = natasha.Segmenter()
        self.morph_vocab = natasha.MorphVocab()
        self.news_embedding = natasha.NewsEmbedding()
        self.news_morph_tagger = natasha.NewsMorphTagger(self.news_embedding)

        self.urls = load_urls()
        self.lemmas_tf_idf = load_lemmas_tf_idf()

    # Метод для выполнения поиска по запросу с возможностью выбора ранжирования.
    def find(self, request):
        keys = parse_request(request, self.segmenter, self.news_morph_tagger, self.morph_vocab)

        # Подсчитываем частоты лемм и вычисляем их TF-IDF.
        lemmas = parse_lemmas(keys, self.segmenter, self.news_morph_tagger, self.morph_vocab)
        tf_idf = calculate_tf_idf(keys, lemmas)

        sorted_results = vector_search(self.lemmas_tf_idf, tf_idf)

        top_10_results = [(self.urls[index], score) for index, score in sorted_results[:10]]

        return top_10_results
