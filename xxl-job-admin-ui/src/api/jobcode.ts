import request from '@/utils/request'

export interface JobCode {
  id: number
  jobId: number
  glueType: string
  glueSource: string
  glueRemark: string
  addTime: string
  updateTime: string
}

export const jobcodeApi = {
  save: (id: number, glueSource: string, glueRemark: string) =>
    request.post<any, string>('/jobcode', null, {
      params: { id, glueSource, glueRemark }
    })
}
