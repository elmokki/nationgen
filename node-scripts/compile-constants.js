const fs = require("fs");
const fsp = fs.promises;
const path = require("path");
const { glob } = require("glob");
const es = require("event-stream");

const OUTPUT_DIR = path.resolve(process.cwd(), "data", "constants");
const MATCH_OBJECTS = [
    {
        name: "filters",
        filePattern: "data/filters/**/*.txt",
        matchRegex: /#id\s+"?([^"]+)"?/
    },
    {
        name: "items",
        filePattern: "data/items/**/*.txt",
        matchRegex: /#id\s+"?([^"]+)"?/
    },
    {
        name: "poses",
        filePattern: "data/poses/**/*.txt",
        matchRegex: /#id\s+"?([^"]+)"?/
    },
    {
        name: "races",
        filePattern: "data/races/**/*.txt",
        matchRegex: /#id\s+"?([^"]+)"?/
    },
    {
        name: "themes",
        filePattern: "data/themes/**/*.txt",
        matchRegex: /#id\s+"?([^"]+)"?/
    }
]

async function compile() {
    if (fs.existsSync(OUTPUT_DIR) === false) {
        await fsp.mkdir(OUTPUT_DIR);
    }

    for (const matchObject of MATCH_OBJECTS) {
        const { name, filePattern, matchRegex } = matchObject;
        const files = await glob(`${process.cwd()}/${filePattern}`);
        const results = new Set();

        for (const filePath of files) {
            await parseFileByLines(filePath, (line) => {
                const match = line.match(matchRegex);
        
                if (match != null) {
                    results.add(match[1]);
                }
            });
        }
        
        await fsp.writeFile(path.resolve(OUTPUT_DIR, `${name}.json`), JSON.stringify(Array.from(results).sort(), null, 2));
    }
}

function parseFileByLines(filepath, lineProcessingFunction) {
    
    // Start measuring amount of ms to parse the whole file
    const start = performance.now();
    console.log(`Parsing "${filepath}"...`);
    
    return new Promise((resolve, reject) => {
        // Start a read stream on file
        const stream = fs.createReadStream(filepath)
            // Use event-stream module to break up every stream chunk into a line
            .pipe(es.split())
            // Iterate through every line in the file
            .pipe(es.mapSync(async (line) => {
        
                // Pause the readstream
                stream.pause();

                // Perform processing for this line
                await lineProcessingFunction(line);
        
                // Resume the readstream to get the next line
                stream.resume();
            })
            .on("error", function(err){
                reject(err);
            })
            // Finished parsing whole file; do post-processing here
            .on("end", function(){
                const end = performance.now();
                console.log(`Finished parsing lines. Total time taken: ${(end - start).toFixed(2)}ms.`);
                resolve();
            })
        );
    });
};

compile();
