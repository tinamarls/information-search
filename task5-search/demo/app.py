from flask import Flask, render_template, request
import sys
import os

# Add the parent directory to the Python path to import the search system
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from search_system import SearchSystem

app = Flask(__name__)
search_system = SearchSystem()

@app.route('/', methods=['GET', 'POST'])
def search():
    results = []
    query = ''
    if request.method == 'POST':
        query = request.form['query']
        if query:
            results = search_system.find(query)
    return render_template('search.html', results=results, query=query)

def run_demo():
    """Run the demo web application."""
    app.run(debug=True)

if __name__ == '__main__':
    run_demo() 