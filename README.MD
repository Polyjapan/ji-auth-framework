JIAuthFramework
===============

A light API to use the JI Auth service.

## How to use?

You need to compile and distribute it locally to be able to use it. It's a simple sbt command:

    sbt packageBin
    sbt publishLocal
    

## Use in a PHP application

Just require the class and create an instance of it.

    require 'JIAuth.class.php';
    
    $baseUrl = "https://auth.japan-impact.ch/";
    $clientId = "client_id";
    $clientSecret = "client_secret";
    
    $auth = new JIAuth($baseUrl, $clientId, $clientSecret);
    
The methods are pretty easy to use.

## Use in a play application

You will want to have a module in your app (see play documentation). In this app, you'll need a method to
provide the authClient.

    
    @Provides
    def provideAuthClient(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration): AuthApi = AuthApi(ws)
    
This version reads values from the config file

    jiauth {
      clientId = ""
      clientSecret = ""
      baseUrl = ""
    }

You can also provide these values directly
    
    @Provides
    def provideAuthClient(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration): AuthApi = {
        new AuthApi(ws, appBaseUrl, clientId, clientSecret)
    }

The usage is pretty straightforward and well documented, see `AuthApi.scala`.