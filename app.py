from flask import Flask
import os

app = Flask(__name__)

i = 0

@app.route('/<float:loudness>/<float:lng>/<float:lat>', methods=['POST'])
def main(loudness, lng, lat):
    if loudness and lng and lat:
        data = []
        with open('data.geojson', 'r') as file:
            data = file.readlines()

        with open('data.geojson', 'w') as file:
            for row in data[:-3]:
                file.write(row)
            file.write("""},
            {
                "geometry": {
                    "type": "Point",
                    "coordinates": [
                        {lng},
                        {lat}
                    ]
                },
                "type": "Feature",
                "properties": [
                    "loudness": {loudness}
                ]
            """)
            for row in data[-3:]:
                file.write(row)
        os.system("export MAPBOX_ACCESS_TOKEN=sk.eyJ1Ijoia3ViYWtpMTgiLCJhIjoiY2tnb3g2d2t0M2plMDJyczFjdmx5ajNxbCJ9.GwDQJQHdReRIi_874cppkw | tilesets upload-source kubaki18 loudness-source data.geojson | tilesets publish kubaki18.loudness")
        print("Sent!")