/**
 * 分片上传工具
 *
 * ============================================================
 * 前端做分片上传，要做四件事：
 *   1. 算出整个文件的 MD5（大文件必须分片读，否则会卡死浏览器）
 *   2. 调 check 接口问：能秒传吗？之前传到哪了？
 *   3. 只上传还没传过的分片
 *   4. 全部传完后调 merge 合并
 * ============================================================
 */
import SparkMD5 from 'spark-md5'
import request from '@/api/request'

/** 分片大小：5MB（要和后端配置一致） */
export const CHUNK_SIZE = 5 * 1024 * 1024

/**
 * 计算文件 MD5
 *
 * <p>为什么不直接把整个文件读进内存算：50MB 还行，500MB 就能把浏览器标签页搞崩。
 * 所以这里分片读取，每次只用 FileReader 读 5MB，算完就丢。</p>
 *
 * @param {File} file
 * @param {(percent:number)=>void} onProgress 计算进度回调（0-100）
 * @returns {Promise<string>} 32 位小写 MD5
 */
export function calcFileMd5(file, onProgress) {
  return new Promise((resolve, reject) => {
    const chunks = Math.ceil(file.size / CHUNK_SIZE)
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let current = 0

    reader.onload = (e) => {
      spark.append(e.target.result)
      current++
      if (onProgress) {
        onProgress(Math.round((current / chunks) * 100))
      }
      if (current < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }

    reader.onerror = () => reject(new Error('读取文件失败，请重试'))

    function loadNext() {
      const start = current * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      reader.readAsArrayBuffer(file.slice(start, end))
    }

    if (file.size === 0) {
      reject(new Error('文件内容为空'))
      return
    }
    loadNext()
  })
}

/**
 * 执行完整的分片上传流程
 *
 * @param {File} file 要上传的文件
 * @param {object} handlers
 *   - onProgress(percent, text)  总进度（0-100）与说明文字
 *   - onStage(stage)             当前阶段：hashing / checking / uploading / merging
 * @returns {Promise<number>} 服务端返回的 fileId
 */
export async function chunkUpload(file, handlers = {}) {
  const { onProgress, onStage } = handlers
  const report = (percent, text) => onProgress && onProgress(percent, text)

  // ---------- 第一阶段：算 MD5（占总进度 0-20%） ----------
  onStage && onStage('hashing')
  report(0, '正在计算文件指纹…')
  const fileHash = await calcFileMd5(file, (p) => {
    report(Math.round(p * 0.2), `正在计算文件指纹… ${p}%`)
  })

  const fileName = file.name
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

  // ---------- 第二阶段：检查（秒传 / 断点续传） ----------
  onStage && onStage('checking')
  report(22, '检查服务端是否已有该文件…')
  const check = await request.get('/api/files/chunk/check', {
    params: { fileHash, fileName, fileSize: file.size }
  })

  if (check.exists) {
    onStage && onStage('done')
    report(100, '秒传成功（服务端已有相同文件）')
    return check.fileId
  }

  const uploadedSet = new Set(check.uploadedChunks || [])
  if (uploadedSet.size > 0) {
    report(25, `发现历史进度，已传 ${uploadedSet.size}/${totalChunks} 片，继续上传…`)
  }

  // ---------- 第三阶段：上传分片（占总进度 25-92%） ----------
  onStage && onStage('uploading')
  let sent = 0
  const needSend = totalChunks - uploadedSet.size

  for (let i = 0; i < totalChunks; i++) {
    if (uploadedSet.has(i)) {
      continue // 断点续传：跳过已传过的分片
    }
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)

    const form = new FormData()
    form.append('chunk', file.slice(start, end))
    form.append('fileHash', fileHash)
    form.append('chunkIndex', String(i))
    form.append('totalChunks', String(totalChunks))
    form.append('fileName', fileName)

    await request.post('/api/files/chunk/upload', form)

    sent++
    const percent = 25 + Math.round((sent / needSend) * 67)
    report(percent, `上传分片 ${i + 1}/${totalChunks}`)
  }

  // ---------- 第四阶段：合并（服务端会做 MD5 复验） ----------
  onStage && onStage('merging')
  report(94, '正在合并分片并校验完整性…')
  const merged = await request.post('/api/files/chunk/merge', {
    fileHash,
    fileName,
    fileSize: file.size,
    totalChunks
  })

  onStage && onStage('done')
  report(100, '上传完成')
  return merged.id
}

/**
 * 把字节数格式化成人看得懂的样子
 */
export function formatSize(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit++
  }
  return unit === 0 ? `${bytes} B` : `${size.toFixed(2)} ${units[unit]}`
}
