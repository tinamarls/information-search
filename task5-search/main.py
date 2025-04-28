from search_system import SearchSystem

if __name__ == '__main__':
    search_system = SearchSystem()

    request = "В Лондоне начинается судебный процесс над бывшим губернатором"
    results = search_system.find(request)

    print("Найденные URL:")
    for url in results:
        print(url)
