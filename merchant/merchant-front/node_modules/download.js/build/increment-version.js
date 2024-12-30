const { exec } = require('child_process') 
const fs = require('fs')

function execAndPipe(command) {
  const { stdout, stderr } = exec(command)
  stdout.pipe(process.stdout)
  stderr.pipe(process.stderr)
}

const package = JSON.parse(fs.readFileSync('./package.json', 'utf8'))
package.version = package.version.replace(/^(\d+?\.\d+?\.)(\d+?)$/g, (match, $1, $2) => `${$1}${+$2+1}`)
fs.writeFileSync('./package.json', JSON.stringify(package, null, 2), 'utf8')
console.log(package.version)
