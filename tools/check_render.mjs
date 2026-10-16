import {createRequire} from 'node:module';
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const assets=path.join(project,'app/src/main/assets');
const bundled=process.env.CODEX_NODE_MODULES;
const require=createRequire(bundled ? path.join(bundled,'package.json') : import.meta.url);
const {chromium}=require('playwright');
const course=JSON.parse(fs.readFileSync(path.join(assets,'course.json'),'utf8'));
const homework=JSON.parse(fs.readFileSync(path.join(assets,'homework.json'),'utf8'));
const pages=[...course.lessons,...homework.problems.map(p=>({id:p.id,title:p.title,html:p.promptHtml+p.solutionHtml}))];
const expectedFormulas=pages.reduce((sum,l)=>sum+(l.html.match(/\\\(/g)||[]).length,0);
const source=fs.readFileSync(path.join(project,'app/src/main/java/cn/selfstudy/information/Readers.kt'),'utf8');
const template=source.split('internal fun readerHtml(body: String) = """')[1].split('"""')[0].trim();
const pageHtml=template.replace('$body',pages.map(l=>`<h1 id="${l.id}">${l.title}</h1>${l.html}`).join(''));
const output=path.resolve(project,'../build-content/visual-check');
fs.mkdirSync(output,{recursive:true});
const server=http.createServer((req,res)=>{
    if(req.url==='/') {res.setHeader('content-type','text/html; charset=utf-8');res.end(pageHtml);return;}
    if(req.url==='/mathjax/tex-svg-full.js') {res.setHeader('content-type','application/javascript');res.end(fs.readFileSync(path.join(assets,'mathjax/tex-svg-full.js')));return;}
    res.statusCode=404;res.end();
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const port=server.address().port;
let browser;
try {
    browser=await chromium.launch({headless:true,channel:'msedge'});
    const page=await browser.newPage({viewport:{width:412,height:915},deviceScaleFactor:1});
    const errors=[],remote=[];
    page.on('pageerror',e=>errors.push(e.message));
    await page.route('**/*',route=>{
        if(new URL(route.request().url()).hostname==='127.0.0.1')return route.continue();
        remote.push(route.request().url());return route.abort();
    });
    await page.goto(`http://127.0.0.1:${port}/`);
    await page.waitForFunction(()=>!!window.MathJax?.startup?.promise);
    await page.evaluate(()=>window.MathJax.startup.promise);
    const result=await page.evaluate(()=>({formulas:document.querySelectorAll('mjx-container').length,
        errors:[...document.querySelectorAll('[data-mml-node="merror"]')].map(x=>x.textContent),
        width:document.documentElement.scrollWidth,viewport:innerWidth,
        headings:document.querySelectorAll('h1').length}));
    if(errors.length || remote.length || result.errors.length || result.formulas!==expectedFormulas || result.width>result.viewport)
        throw Error(JSON.stringify({result,errors,remote}));
    await page.locator('#entropy-mean').scrollIntoViewIfNeeded();
    await page.screenshot({path:path.join(output,'course-phone.png')});
    for(const problem of homework.problems) {
        await page.locator(`#${problem.id}`).scrollIntoViewIfNeeded();
        await page.screenshot({path:path.join(output,`${problem.id}-phone.png`)});
    }
    await page.getByRole('heading',{name:'4. 点互信息',exact:true}).scrollIntoViewIfNeeded();
    await page.screenshot({path:path.join(output,'homework-solution-phone.png')});
    fs.writeFileSync(path.join(output,'render-report.json'),JSON.stringify({result,errors,remote},null,2));
    console.log('PASS: '+JSON.stringify(result)+', no external requests.');
} finally {await browser?.close();await new Promise(resolve=>server.close(resolve));}
