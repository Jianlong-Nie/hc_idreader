
# hc-idreader

## Getting started

`$ npm install hc-idreader --save`

### Mostly automatic installation

`$ react-native link hc-idreader`

### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNIdreaderPackage;` to the imports at the top of the file
  - Add `new RNIdreaderPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':hc-idreader'
  	project(':hc-idreader').projectDir = new File(rootProject.projectDir, 	'../node_modules/hc-idreader/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':hc-idreader')
  	```


## Usage
```javascript
import RNIdreader from 'hc-idreader';

// TODO: What to do with the module?
RNIdreader;
```
  