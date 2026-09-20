import { spawn } from 'node:child_process'
import { readdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const importModeFlag = '--import-discovered-tests'

if (process.argv[2] === importModeFlag) {
  for (const file of process.argv.slice(3)) {
    await import(pathToFileURL(path.resolve(file)).href)
  }
} else {
  const sourceDirectory = path.resolve('src')
  const testFiles = (await findTestFiles(sourceDirectory)).sort()

  if (testFiles.length === 0) {
    console.error('No test files found.')
    process.exitCode = 1
  } else {
    process.exitCode = await runDiscoveredTests(testFiles)
  }
}

async function runDiscoveredTests(testFiles) {
  const runnerPath = fileURLToPath(import.meta.url)
  const child = spawn(
    process.execPath,
    ['--experimental-strip-types', runnerPath, importModeFlag, ...testFiles],
    { stdio: 'inherit' },
  )

  return new Promise((resolve, reject) => {
    child.on('error', reject)
    child.on('close', (code, signal) => {
      if (signal) {
        console.error(`Test process exited with signal ${signal}.`)
        resolve(1)
        return
      }

      resolve(code ?? 1)
    })
  })
}

async function findTestFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map(async (entry) => {
    const entryPath = path.join(directory, entry.name)

    if (entry.isDirectory()) {
      return findTestFiles(entryPath)
    }

    if (entry.isFile() && (entry.name.endsWith('.test.ts') || entry.name.endsWith('.test.tsx'))) {
      return [entryPath]
    }

    return []
  }))

  return files.flat()
}
