import config from '../config.js';
import fs from 'fs';
import path from 'path';

const configTreePath = '/etc/liferay/lxc';

async function* walk(dir) {
	if (fs.existsSync(dir) === false) return;
	for await (const dirent of await fs.promises.opendir(dir)) {
		const entryPath = path.join(dir, dirent.name);
		if (dirent.isDirectory()) yield* walk(entryPath);
		else if (dirent.isFile()) yield entryPath;
	}
}

const configMap = async () => {
	for await (const configFile of walk(configTreePath)) {
		const configFileName = configFile.substring(
			configFile.lastIndexOf('/') + 1
		);
		config[configFileName] = fs.readFileSync(configFile, 'utf-8');
	}
	return config;
};

export default await configMap();
