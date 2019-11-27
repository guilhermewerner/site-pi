using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using SitePi.Models;

namespace SitePi.Models
{
    public class Bot
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Name { get; set; }

        public int UserId { get; set; }
        public User User { get; set; }

        [Required]
        public string FileBytes { get; set; }

        [Required]
        public string FileName { get; set; }

        public float PercentageA { get; set; }
        public float PercentageB { get; set; }
        public float PercentageC { get; set; }
        public float PercentageD { get; set; }
    }
}
