package net.hearthstats.modules

import net.hearthstats.modules.upload.DummyFileUploader
import net.hearthstats.modules.upload.FileUploader

class FileUploaderFactory extends ModuleFactory[FileUploader](
  "video uploader",
  classOf[FileUploader],
  classOf[DummyFileUploader])