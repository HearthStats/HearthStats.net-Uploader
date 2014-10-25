package net.hearthstats.modules

import net.hearthstats.modules.upload.DummyFileUploader
import net.hearthstats.modules.upload.FileUploader

class FileUploaderFactory(use: => Boolean) extends ModuleFactory[FileUploader](
  "video uploader",
  classOf[FileUploader],
  use,
  classOf[DummyFileUploader])