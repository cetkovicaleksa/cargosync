import { ModelDto } from './model.dto';

export interface MakeDto {
  wmi: string;
  name: string;
  models: ModelDto[];
}
