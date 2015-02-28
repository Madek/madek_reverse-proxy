
httpProxy = require('http-proxy');

get_config= -> 
  yaml = require('js-yaml');
  fs   = require('fs');
  extend = require('node.extend');
  default_config = 
    try 
      yaml.safeLoad(fs.readFileSync('../config/config_default.yml', 'utf8'))
    catch e
      console.error("failed to parse ../config/config_default.yml")
      process.exit(1)

  config = 
    try 
      yaml.safeLoad(fs.readFileSync('../config/config.yml', 'utf8'))
    catch e
      console.info("failed to parse ../config/config.yml")
      {}

  extend(true, default_config, config)


config = get_config()

console.log config

