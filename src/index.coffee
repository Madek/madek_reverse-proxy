

get_config= -> 
  yaml = require 'js-yaml'
  fs   = require 'fs' 
  extend = require 'node.extend'
  default_config = 
    try 
      yaml.safeLoad(fs.readFileSync('../config/config_default.yml', 'utf8'))
    catch e
      console.error "failed to parse ../config/config_default.yml" 
      process.exit 1

  config = 
    try 
      yaml.safeLoad(fs.readFileSync('../config/config.yml', 'utf8'))
    catch e
      console.info "failed to parse ../config/config.yml"
      {}

  extend true, default_config, config 

config = get_config()

# console.log config
# console.log config.services.builder

escapeRegExp= (string)-> 
  string.replace(/([.*+?^${}()|\[\]\/\\])/g, "\\$1")

http = require 'http'
httpProxy = require 'http-proxy'
proxy= httpProxy.createProxyServer({})
proxy_port= config.dev_proxy.http.port

service_dispatch_config= Object.keys(config.services).map (k)->
  http_conf= config.services[k].http
  prefix= escapeRegExp "#{http_conf.context}#{http_conf.sub_context}"
  regex = new RegExp "^#{prefix}.*$"
  uri = (if http_conf.ssl then "https" else "http") + "://" + http_conf.host + ":" + http_conf.port  
  [regex, uri]


select_target= (req)->
  service_dispatch_config.map( ([regex,uri])->
    if req.url.match(regex) then uri else null).filter( (e) -> e?)[0]

# console.info service_dispatch_config

console.info "starting dev proxy on #{proxy_port}"

server = http.createServer (req,res)->
  if target = select_target(req)
    console.info "proxying #{req.method} #{req.url} to #{target}"
    proxy.web(req,res,{target: target})
  else
    console.warn "not matching target for #{req.url}"
    res.writeHead(500, {"Content-Type": "text/plain"});
    res.write("PROXY ERROR not matching target for #{req.url}");
    res.end()

server.listen(proxy_port)
